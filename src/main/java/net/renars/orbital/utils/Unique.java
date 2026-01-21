package net.renars.orbital.utils;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Nodrošina, ka parametrs ir unikāls savā kontekstā.
 * --Renars
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.PARAMETER)
public @interface Unique {
}
