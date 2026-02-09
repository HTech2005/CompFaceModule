#!/usr/bin/env python3
import csv
import os
import re
from collections import defaultdict
import numpy as np
from sklearn import metrics
import matplotlib
matplotlib.use('Agg')
import matplotlib.pyplot as plt

ROOT = os.path.abspath(os.path.join(os.path.dirname(__file__), '..'))
CSV = os.path.join(ROOT, 'target', 'full_benchmark_results.csv')
OUTDIR = os.path.join(ROOT, 'target', 'figures')
os.makedirs(OUTDIR, exist_ok=True)


def parse_num(s):
    if s is None:
        return 0.0
    s = s.strip()
    if s == '':
        return 0.0
    # replace comma decimal separators with dot, also remove thousand separators
    s = s.replace(' ', '')
    # If there are multiple commas and dots, unify by replacing commas with dots
    if s.count(',') > 0 and s.count('.') == 0:
        s = s.replace(',', '.')
    # remove any characters except digits, dot, minus
    s = re.sub(r"[^0-9.\-]", '', s)
    try:
        return float(s)
    except:
        return 0.0


def person_id(filename):
    base = filename.split('/')[-1].split('\\')[-1]
    return base.split('_')[0] if '_' in base else os.path.splitext(base)[0]


def read_rows(path):
    rows = []
    with open(path, newline='', encoding='utf-8') as f:
        reader = csv.DictReader(f)
        for r in reader:
            rows.append(r)
    return rows


def compute_basic(rows):
    y_true = []
    scores = []
    chi = []
    cos = []
    eu = []
    for r in rows:
        file = r.get('file','')
        found = r.get('found','').lower() in ('true','1','yes','y')
        # fallback: compute from bestMatch
        if not found:
            best = r.get('bestMatch','')
            if best:
                found = (person_id(file) == person_id(best))
        y_true.append(1 if found else 0)
        scores.append(parse_num(r.get('score','0')))
        chi.append(parse_num(r.get('scoreChi2','0')))
        cos.append(parse_num(r.get('scoreCos','0')))
        eu.append(parse_num(r.get('scoreEucl','0')))
    return np.array(y_true), np.array(scores), np.array(chi), np.array(cos), np.array(eu)


def plot_roc(y_true, scores, out_png, out_txt):
    fpr, tpr, thr = metrics.roc_curve(y_true, scores)
    auc = metrics.auc(fpr, tpr)
    plt.figure(figsize=(6,6))
    plt.plot(fpr, tpr, label=f'AUC = {auc:.4f}')
    plt.plot([0,1],[0,1],'k--',alpha=0.4)
    plt.xlabel('FPR')
    plt.ylabel('TPR')
    plt.title('ROC curve')
    plt.legend(loc='lower right')
    plt.grid(True, alpha=0.3)
    plt.tight_layout()
    plt.savefig(out_png)
    plt.close()
    # write summary
    with open(out_txt, 'w', encoding='utf-8') as f:
        f.write(f'AUC: {auc:.4f}\n')
    return auc


def compute_eer(fpr, tpr, thr):
    # EER where FNR ~= FPR -> FNR = 1 - TPR
    fnr = 1 - tpr
    abs_diffs = np.abs(fnr - fpr)
    idx = np.argmin(abs_diffs)
    return (fpr[idx] + fnr[idx]) / 2.0, thr[idx]


def plot_score_distribution(scores, y_true, out_png):
    plt.figure(figsize=(6,4))
    plt.hist(scores[y_true==0], bins=30, alpha=0.6, label='impostor')
    plt.hist(scores[y_true==1], bins=30, alpha=0.6, label='genuine')
    plt.xlabel('Score')
    plt.ylabel('Count')
    plt.legend()
    plt.title('Score distribution')
    plt.tight_layout()
    plt.savefig(out_png)
    plt.close()


def grid_search_weights(chi, cos, eu, y_true):
    best = None
    results = []
    # grid with step 0.1 and enforce sum>0 then normalize
    steps = np.arange(0.0, 1.01, 0.1)
    for w1 in steps:
        for w2 in steps:
            for w3 in steps:
                s = w1 + w2 + w3
                if s == 0: continue
                wchi = w1 / s
                wcos = w2 / s
                weu = w3 / s
                fused = (chi * wchi) + (cos * wcos) + (eu * weu)
                # pick best threshold by F1 over unique scores
                unique_thr = np.unique(fused)
                best_f1 = -1.0
                best_thr = None
                for thr in unique_thr:
                    preds = (fused >= thr).astype(int)
                    tp = ((preds==1) & (y_true==1)).sum()
                    fp = ((preds==1) & (y_true==0)).sum()
                    fn = ((preds==0) & (y_true==1)).sum()
                    prec = tp / (tp+fp) if (tp+fp)>0 else 0.0
                    rec = tp / (tp+fn) if (tp+fn)>0 else 0.0
                    f1 = 2*prec*rec/(prec+rec) if (prec+rec)>0 else 0.0
                    if f1 > best_f1:
                        best_f1 = f1
                        best_thr = thr
                # evaluate metrics at best_thr
                preds = (fused >= best_thr).astype(int)
                tp = ((preds==1) & (y_true==1)).sum()
                fp = ((preds==1) & (y_true==0)).sum()
                tn = ((preds==0) & (y_true==0)).sum()
                fn = ((preds==0) & (y_true==1)).sum()
                FAR = 100.0 * fp / (fp + tn) if (fp+tn)>0 else 0.0
                FRR = 100.0 * fn / (tp + fn) if (tp+fn)>0 else 0.0
                auc = metrics.roc_auc_score(y_true, fused)
                fpr, tpr, thrv = metrics.roc_curve(y_true, fused)
                eer, eer_thr = compute_eer(fpr, tpr, thrv)
                results.append(((wchi,wcos,weu), best_thr, best_f1, tp, fp, tn, fn, FAR, FRR, auc, eer))
                if best is None or best_f1 > best[2]:
                    best = ((wchi,wcos,weu), best_thr, best_f1, tp, fp, tn, fn, FAR, FRR, auc, eer)
    return best, results


def save_fusion_summary(best, results, out_txt):
    with open(out_txt, 'w', encoding='utf-8') as f:
        f.write('Best weights (w_chi, w_cos, w_eucl) and metrics:\n')
        w, thr, f1, tp, fp, tn, fn, FAR, FRR, auc, eer = best
        f.write(f'weights: {w}, threshold: {thr}, F1: {f1:.4f}\n')
        f.write(f"tp: {tp}, fp: {fp}, tn: {tn}, fn: {fn}, FAR: {FAR:.4f}, FRR: {FRR:.4f}, AUC: {auc:.4f}, EER: {eer:.4f}\n\n")
        f.write('All results (weights,thr,F1,tp,fp,tn,fn,FAR,FRR,AUC,EER)\n')
        for row in sorted(results, key=lambda r: -r[2]):
            w, thr, f1, tp, fp, tn, fn, FAR, FRR, auc, eer = row
            f.write(f"{w},{thr},{f1:.4f},{tp},{fp},{tn},{fn},{FAR:.4f},{FRR:.4f},{auc:.4f},{eer:.4f}\n")


def compare_with_decision(best, rows, out_txt):
    # Decision constants are in Java; attempt to import via reading Decision.java if present
    dec_file = os.path.join(ROOT, 'src', 'main', 'java', 'tech', 'HTECH', 'Decision.java')
    wchi = wcos = weu = None
    thr = None
    if os.path.exists(dec_file):
        with open(dec_file, 'r', encoding='utf-8') as f:
            txt = f.read()
        m1 = re.search(r'W_CHI\s*=\s*([0-9.]+)', txt)
        m2 = re.search(r'W_COS\s*=\s*([0-9.]+)', txt)
        m3 = re.search(r'W_EUCL\s*=\s*([0-9.]+)', txt)
        m4 = re.search(r'THRESHOLD\s*=\s*([0-9.]+)', txt)
        if m1 and m2 and m3:
            wchi = float(m1.group(1))
            wcos = float(m2.group(1))
            weu = float(m3.group(1))
        if m4:
            thr = float(m4.group(1))
    # compute metrics for Decision weights if available
    rows_arr = rows
    y_true, scores, chi, cosv, euv = compute_basic(rows_arr)
    if wchi is not None:
        fused = chi*wchi + cosv*wcos + euv*weu
        if thr is None:
            # choose thr maximizing F1
            unique_thr = np.unique(fused)
            best_f1 = -1
            best_t = None
            for t in unique_thr:
                preds = (fused >= t).astype(int)
                tp = ((preds==1) & (y_true==1)).sum()
                fp = ((preds==1) & (y_true==0)).sum()
                fn = ((preds==0) & (y_true==1)).sum()
                prec = tp/(tp+fp) if (tp+fp)>0 else 0.0
                rec = tp/(tp+fn) if (tp+fn)>0 else 0.0
                f1 = 2*prec*rec/(prec+rec) if (prec+rec)>0 else 0.0
                if f1>best_f1:
                    best_f1=f1
                    best_t=t
            thr = best_t
        preds = (fused >= thr).astype(int)
        tp = ((preds==1) & (y_true==1)).sum()
        fp = ((preds==1) & (y_true==0)).sum()
        tn = ((preds==0) & (y_true==0)).sum()
        fn = ((preds==0) & (y_true==1)).sum()
        FAR = 100.0 * fp / (fp + tn) if (fp+tn)>0 else 0.0
        FRR = 100.0 * fn / (tp + fn) if (tp+fn)>0 else 0.0
        auc = metrics.roc_auc_score(y_true, fused)
        fpr, tpr, thrv = metrics.roc_curve(y_true, fused)
        eer, eer_thr = compute_eer(fpr, tpr, thrv)
        # compute F1 at chosen threshold
        prec = tp/(tp+fp) if (tp+fp)>0 else 0.0
        rec = tp/(tp+fn) if (tp+fn)>0 else 0.0
        f1_at_thr = 2*prec*rec/(prec+rec) if (prec+rec)>0 else 0.0
        with open(out_txt, 'w', encoding='utf-8') as f:
            f.write(f'Decision weights from {dec_file} -> w_chi={wchi}, w_cos={wcos}, w_eucl={weu}, threshold={thr}\n')
            f.write(f'tp: {tp}, fp: {fp}, tn: {tn}, fn: {fn}, FAR: {FAR:.4f}, FRR: {FRR:.4f}, F1: {f1_at_thr:.4f}, AUC: {auc:.4f}, EER: {eer:.4f}\n')
    else:
        with open(out_txt, 'w', encoding='utf-8') as f:
            f.write(f'Could not read Decision.java at {dec_file} or weights not found. No comparison made.\n')


def main():
    if not os.path.exists(CSV):
        print('CSV not found:', CSV)
        return
    rows = read_rows(CSV)
    y_true, scores, chi, cosv, euv = compute_basic(rows)

    # ROC & distribution
    auc = plot_roc(y_true, scores, os.path.join(OUTDIR, 'roc_curve.png'), os.path.join(OUTDIR, 'roc_info.txt'))
    plot_score_distribution(scores, y_true, os.path.join(OUTDIR, 'score_distribution.png'))

    # grid-search
    best, results = grid_search_weights(chi, cosv, euv, y_true)
    save_fusion_summary(best, results, os.path.join(OUTDIR, 'fusion_search_summary.txt'))

    # compare with Decision.java
    compare_with_decision(rows, rows, os.path.join(OUTDIR, 'weights_test.txt'))

    print('Analysis complete. Outputs in', OUTDIR)

if __name__ == '__main__':
    main()
