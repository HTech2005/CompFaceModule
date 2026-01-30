package tech.HTECH.ui;

/**
 * Interface permettant aux contrôleurs de libérer des ressources 
 * ou d'arrêter des threads en arrière-plan lors d'un changement de vue.
 */
public interface Stoppable {
    void stopBackgroundTasks();
}
