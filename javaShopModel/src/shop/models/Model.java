/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
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
