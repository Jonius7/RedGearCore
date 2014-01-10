package redgear.core.api.reflection;

import java.lang.reflect.InvocationTargetException;

public class ReflectionHelper {

	public static Class<?> getClass(String name) throws ClassNotFoundException {
		return Class.forName(name);
	}

	public static Object constructObject(Class<?> clazz, Object... args) throws InstantiationException,
			IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException,
			SecurityException {
		return clazz.getConstructor(getTypes(args)).newInstance(args);
	}

	public static Object constructObject(String name, Object... args) throws InstantiationException,
			IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException,
			SecurityException, ClassNotFoundException {
		return getClass(name).getConstructor(getTypes(args)).newInstance(args);
	}
	
	public static Object constructObjectNullFail(Class<?> clazz, Object... args){
		try{
			return constructObject(clazz, args);
		}
		catch(Exception e){
			return null;
		}
	}
	
	public static Object constructObjectNullFail(String name, Object... args){
		try{
			return constructObject(name, args);
		}
		catch(Exception e){
			return null;
		}
	}

	public static Class<?>[] getTypes(Object... args) {
		Class<?>[] argTypes = new Class[args.length];

		for (int i = 0; i < args.length; i++)
			argTypes[i] = args[i].getClass();

		return argTypes;
	}
}
