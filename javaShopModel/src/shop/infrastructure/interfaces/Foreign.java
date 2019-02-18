/*
 * Kalin Stoyanov, Lyuboslav Angelov 2019
 * Licensed under MIT license. See LICENSE for full text
 */
package shop.infrastructure.interfaces;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * foreign key that will be used for splitting lists of models
 * @author k
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface Foreign {
    public Class comparator();
}
