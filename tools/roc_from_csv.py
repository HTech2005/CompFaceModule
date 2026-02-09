#!/usr/bin/env python3
"""
Simple ROC/AUC and threshold selection from benchmark_results.csv.
Usage: python tools/roc_from_csv.py target/benchmark_results.csv
Produces: target/roc_summary.txt
"""
import sys
import csv
from math import fabs

def person_id(filename):
    # assume format PersonneXX_YY.jpg or PersonneXX_YY.jpeg
    base = filename.split('/')[-1].split('\\')[-1]
    parts = base.split('_')
    if len(parts) == 0:
        return base
    return parts[0]


def read_csv(path):
    rows = []
    with open(path, newline='', encoding='utf-8') as f:
        reader = csv.DictReader(f)
        for r in reader:
            rows.append(r)
    return rows


def compute_scores(rows):
    y_true = []
    scores = []
    for r in rows:
        file = r.get('file','')
        best = r.get('bestMatch','')
        try:
            score = float(r.get('score','0'))
        except:
            score = 0.0
        # genuine if bestMatch starts with same person id
        true_label = 1 if best and person_id(file) == person_id(best) else 0
        y_true.append(true_label)
        scores.append(score)
    return y_true, scores


def roc(y_true, scores):
    # compute TPR/FPR for thresholds
    data = sorted(zip(scores, y_true), key=lambda x: x[0], reverse=True)
    P = sum(y_true)
    N = len(y_true) - P
    tps = 0
    fps = 0
    roc_points = []
    prev_score = None
    for s, y in data:
        if y == 1:
            tps += 1
        else:
            fps += 1
        # record point at this score
        tpr = tps / P if P>0 else 0.0
        fpr = fps / N if N>0 else 0.0
        roc_points.append((fpr, tpr, s))
    # auc via trapezoid
    auc = 0.0
    prev_fpr, prev_tpr = 0.0, 0.0
    for fpr, tpr, _ in roc_points:
        auc += (fpr - prev_fpr) * (tpr + prev_tpr) / 2.0
        prev_fpr, prev_tpr = fpr, tpr
    return roc_points, auc


def find_best_threshold(y_true, scores):
    # maximize F1
    best_f1 = -1.0
    best_thr = None
    for thr in sorted(set(scores)):
        preds = [1 if s >= thr else 0 for s in scores]
        tp = sum(1 for p,t in zip(preds, y_true) if p==1 and t==1)
        fp = sum(1 for p,t in zip(preds, y_true) if p==1 and t==0)
        fn = sum(1 for p,t in zip(preds, y_true) if p==0 and t==1)
        prec = tp / (tp+fp) if (tp+fp)>0 else 0.0
        rec = tp / (tp+fn) if (tp+fn)>0 else 0.0
        f1 = 2*prec*rec/(prec+rec) if (prec+rec)>0 else 0.0
        if f1 > best_f1:
            best_f1 = f1
            best_thr = thr
    return best_thr, best_f1


def main():
    if len(sys.argv) < 2:
        print('Usage: python tools/roc_from_csv.py target/benchmark_results.csv')
        return
    path = sys.argv[1]
    rows = read_csv(path)
    if not rows:
        print('No rows in CSV')
        return
    y_true, scores = compute_scores(rows)
    roc_points, auc = roc(y_true, scores)
    best_thr, best_f1 = find_best_threshold(y_true, scores)
    out = 'target/roc_summary.txt'
    with open(out, 'w', encoding='utf-8') as f:
        f.write(f'AUC: {auc:.4f}\n')
        f.write(f'Best threshold (max F1): {best_thr} (F1={best_f1:.4f})\n')
        f.write('ROC points (fpr,tpr,score)\n')
        for fpr,tpr,s in roc_points:
            f.write(f'{fpr:.4f},{tpr:.4f},{s:.4f}\n')
    print('Wrote', out)

if __name__ == "__main__":
    main()
