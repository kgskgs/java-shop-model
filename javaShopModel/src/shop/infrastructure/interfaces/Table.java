/*
 * Kalin Stoyanov, Lyuboslav Angelov 2019
 * Licensed under MIT license. See LICENSE for full text
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
