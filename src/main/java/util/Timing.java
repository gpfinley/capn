package util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.logging.Logger;

/**
 * Simple utility function for running time measurements
 *
 * Created by gpfinley on 10/10/16.
 */
public class Timing {

    private final static Logger LOGGER = Logger.getLogger(Timing.class.getName());

    public static long timeMethod(Object object, Method method, Object... args) {
        long time = System.nanoTime();
        try {
            method.invoke(object, args);
        } catch (IllegalAccessException e) {
            LOGGER.severe("Illegal access exception");
            System.exit(1);
        } catch (InvocationTargetException e) {
            LOGGER.severe("Invocation target exception");
            System.exit(1);
        }
        return System.nanoTime() - time;
    }
}
