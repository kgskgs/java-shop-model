/*
 * Kalin Stoyanov, Lyuboslav Angelov 2019
 * Licensed under MIT license. See LICENSE for full text
 */
package shop.models;

/**
 *
 * @author k
 */
public abstract class Model {
    
    public Model() {}
    
    //flags for ui interactions; protected so repository ignores them
    protected boolean newInstance = false;
    protected boolean updated = false;

    public boolean isNewInstance() {
        return newInstance;
    }

    public void setNewInstance(boolean newInstance) {
        this.newInstance = newInstance;
    }

    public boolean isUpdated() {
        return updated;
    }

    public void setUpdated(boolean updated) {
        this.updated = updated;
    }
    
}
