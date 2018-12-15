/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package shop.infrastructure.interfaces;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Comparator;

/**
 * foreign key that will be used for splitting lists of models
 * @author k
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface Foreign {
    public Class comparator();
}
