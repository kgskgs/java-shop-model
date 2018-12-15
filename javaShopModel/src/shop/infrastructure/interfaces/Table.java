/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package shop.infrastructure.interfaces;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * name of the mysql table associated with the model
 * @author Lyuboslav
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface Table {
    public String Name();
}
