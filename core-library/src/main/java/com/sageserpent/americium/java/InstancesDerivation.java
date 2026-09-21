package com.sageserpent.americium.java;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import cyclops.control.Either;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.dynamic.scaffold.subclass.ConstructorStrategy;
import net.bytebuddy.implementation.FieldAccessor;
import net.bytebuddy.implementation.Implementation;
import net.bytebuddy.implementation.MethodCall;
import net.bytebuddy.matcher.ElementMatchers;

import java.lang.reflect.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.util.*;

public class InstancesDerivation {
    private static final Cache<Type, Trials<?>> cache = Caffeine.newBuilder().build();

    @SuppressWarnings("unchecked")
    public static <Case> Trials<Case> instances(Class<Case> clazz) {
        return (Trials<Case>) cache.get(clazz, InstancesDerivation::createTrialsForType);
    }

    @SuppressWarnings("unchecked")
    public static <Case> Trials<Case> instances(Type type) {
        return (Trials<Case>) cache.get(type, InstancesDerivation::createTrialsForType);
    }

    private static <T> Trials<T> delayInstances(Type type) {
        return Trials.api().delay(() -> instances(type));
    }

    private static Trials<?> createTrialsForType(Type type) {
        Class<?> rawClass = getRawClass(type);

        Trials<?> baseTrial = baseTypeTrial(rawClass);
        if (baseTrial != null) {
            return baseTrial;
        }

        if (rawClass.isEnum()) {
            Object[] constants = rawClass.getEnumConstants();
            if (constants == null || constants.length == 0) {
                return Trials.api().impossible();
            }
            if (constants.length == 1) {
                return Trials.api().only(constants[0]);
            }
            return Trials.api().choose(constants);
        }

        if (rawClass.isArray()) {
            Class<?> componentType = rawClass.getComponentType();
            Trials<Object> componentTrial = delayInstances(componentType);
            return componentTrial.immutableLists().map(list -> {
                Object array = Array.newInstance(componentType, list.size());
                for (int i = 0; i < list.size(); i++) {
                    Array.set(array, i, list.get(i));
                }
                return array;
            });
        }

        Trials<?> containerTrial = containerTypeTrial(rawClass, type);
        if (containerTrial != null) {
            return containerTrial;
        }

        Class<?> targetClass = rawClass;
        if (Modifier.isAbstract(rawClass.getModifiers()) || rawClass.isInterface()) {
            targetClass = generateByteBuddyImplementation(rawClass);
        }

        Constructor<?>[] constructors = targetClass.getConstructors();
        if (constructors.length == 0) {
            constructors = targetClass.getDeclaredConstructors();
        }

        if (constructors.length == 0) {
            throw new IllegalArgumentException("Cannot automatically derive Trials for " + type + ": no constructors found.");
        }

        List<Trials<Object>> constructorTrialsList = new ArrayList<>();
        for (Constructor<?> constructor : constructors) {
            ConstructorInvoker invoker = createConstructorInvoker(constructor);
            Type[] genericParamTypes = constructor.getGenericParameterTypes();
            if (genericParamTypes.length == 0) {
                constructorTrialsList.add(Trials.api().delay(() -> {
                    try {
                        return Trials.api().only(invoker.invoke(new Object[0]));
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to invoke constructor " + constructor, e);
                    }
                }));
            } else {
                List<Trials<Object>> paramTrials = new ArrayList<>();
                for (Type paramType : genericParamTypes) {
                    Type resolvedType = resolveType(paramType, type, rawClass);
                    Trials<Object> paramTrial = delayInstances(resolvedType);
                    paramTrials.add(paramTrial);
                }
                Trials<Object> constructorTrial = Trials.api().immutableLists(paramTrials).map(args -> {
                    try {
                        return invoker.invoke(args.toArray());
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to invoke constructor " + constructor + " with args " + args, e);
                    }
                });
                constructorTrialsList.add(constructorTrial);
            }
        }

        if (constructorTrialsList.size() == 1) {
            return constructorTrialsList.get(0);
        } else {
            @SuppressWarnings("unchecked")
            Trials<Object>[] array = constructorTrialsList.toArray(new Trials[0]);
            return Trials.api().alternate(array[0], array[1], Arrays.copyOfRange(array, 2, array.length));
        }
    }

    private static Trials<?> baseTypeTrial(Class<?> rawClass) {
        if (rawClass == Integer.class || rawClass == int.class) {
            return Trials.api().integers();
        } else if (rawClass == Long.class || rawClass == long.class) {
            return Trials.api().longs();
        } else if (rawClass == Double.class || rawClass == double.class) {
            return Trials.api().doubles();
        } else if (rawClass == Boolean.class || rawClass == boolean.class) {
            return Trials.api().booleans();
        } else if (rawClass == Byte.class || rawClass == byte.class) {
            return Trials.api().bytes();
        } else if (rawClass == Character.class || rawClass == char.class) {
            return Trials.api().characters();
        } else if (rawClass == Short.class || rawClass == short.class) {
            return Trials.api().integers((int) Short.MIN_VALUE, (int) Short.MAX_VALUE).map(Integer::shortValue);
        } else if (rawClass == Float.class || rawClass == float.class) {
            return Trials.api().doubles().map(Double::floatValue);
        } else if (rawClass == String.class) {
            return Trials.api().strings();
        } else if (rawClass == BigInteger.class) {
            return Trials.api().bigIntegers(BigInteger.valueOf(Long.MIN_VALUE), BigInteger.valueOf(Long.MAX_VALUE));
        } else if (rawClass == BigDecimal.class) {
            return Trials.api().bigDecimals(BigDecimal.valueOf(Double.MIN_VALUE), BigDecimal.valueOf(Double.MAX_VALUE));
        } else if (rawClass == Instant.class) {
            return Trials.api().instants();
        }
        return null;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Trials<?> containerTypeTrial(Class<?> rawClass, Type type) {
        if (Optional.class.isAssignableFrom(rawClass)) {
            Type elemType = getSingleTypeArgument(type, Object.class);
            Trials<Object> elemTrial = delayInstances(elemType);
            return elemTrial.optionals();
        } else if (List.class.isAssignableFrom(rawClass) || Iterable.class == rawClass || Collection.class == rawClass) {
            Type elemType = getSingleTypeArgument(type, Object.class);
            Trials<Object> elemTrial = delayInstances(elemType);
            return elemTrial.immutableLists();
        } else if (Set.class.isAssignableFrom(rawClass)) {
            Type elemType = getSingleTypeArgument(type, Object.class);
            Trials<Object> elemTrial = delayInstances(elemType);
            return elemTrial.immutableSets();
        } else if (Map.class.isAssignableFrom(rawClass)) {
            Type keyType = getTypeArgument(type, 0, Object.class);
            Type valueType = getTypeArgument(type, 1, Object.class);
            Trials<Object> keyTrial = delayInstances(keyType);
            Trials<Object> valTrial = delayInstances(valueType);
            return keyTrial.immutableMaps(valTrial);
        } else if (Either.class.isAssignableFrom(rawClass)) {
            Type leftType = getTypeArgument(type, 0, Object.class);
            Type rightType = getTypeArgument(type, 1, Object.class);
            Trials<Object> leftTrial = delayInstances(leftType);
            Trials<Object> rightTrial = delayInstances(rightType);
            return leftTrial.or(rightTrial);
        }
        return null;
    }

    private static Class<?> getRawClass(Type type) {
        if (type instanceof Class<?>) {
            return (Class<?>) type;
        } else if (type instanceof ParameterizedType) {
            return (Class<?>) ((ParameterizedType) type).getRawType();
        } else if (type instanceof GenericArrayType) {
            Type componentType = ((GenericArrayType) type).getGenericComponentType();
            Class<?> rawComponent = getRawClass(componentType);
            return Array.newInstance(rawComponent, 0).getClass();
        } else if (type instanceof TypeVariable<?>) {
            Type[] bounds = ((TypeVariable<?>) type).getBounds();
            return bounds.length > 0 ? getRawClass(bounds[0]) : Object.class;
        } else if (type instanceof WildcardType) {
            Type[] upperBounds = ((WildcardType) type).getUpperBounds();
            return upperBounds.length > 0 ? getRawClass(upperBounds[0]) : Object.class;
        }
        return Object.class;
    }

    private static Type getSingleTypeArgument(Type type, Type fallback) {
        return getTypeArgument(type, 0, fallback);
    }

    private static Type getTypeArgument(Type type, int index, Type fallback) {
        if (type instanceof ParameterizedType) {
            Type[] args = ((ParameterizedType) type).getActualTypeArguments();
            if (index < args.length) {
                return args[index];
            }
        }
        return fallback;
    }

    private static Type resolveType(Type paramType, Type ownerType, Class<?> ownerClass) {
        if (paramType instanceof Class<?>) {
            return paramType;
        } else if (paramType instanceof TypeVariable<?>) {
            TypeVariable<?> typeVar = (TypeVariable<?>) paramType;
            TypeVariable<?>[] ownerParams = ownerClass.getTypeParameters();
            for (int i = 0; i < ownerParams.length; i++) {
                if (ownerParams[i].getName().equals(typeVar.getName())) {
                    return getTypeArgument(ownerType, i, typeVar);
                }
            }
            return typeVar;
        } else if (paramType instanceof ParameterizedType) {
            ParameterizedType pType = (ParameterizedType) paramType;
            Type[] args = pType.getActualTypeArguments();
            Type[] resolvedArgs = new Type[args.length];
            for (int i = 0; i < args.length; i++) {
                resolvedArgs[i] = resolveType(args[i], ownerType, ownerClass);
            }
            return new ParameterizedType() {
                @Override
                public Type[] getActualTypeArguments() { return resolvedArgs; }
                @Override
                public Type getRawType() { return pType.getRawType(); }
                @Override
                public Type getOwnerType() { return pType.getOwnerType(); }
            };
        }
        return paramType;
    }

    private static ConstructorInvoker createConstructorInvoker(Constructor<?> constructor) {
        try {
            Class<?> declaringClass = constructor.getDeclaringClass();
            var byteBuddy = new ByteBuddy();
            Class<?> invokerClass = byteBuddy
                    .subclass(Object.class)
                    .implement(ConstructorInvoker.class)
                    .name(String.format("%s$ByteBuddyInvoker$%d", declaringClass.getName(), Math.abs(constructor.hashCode())))
                    .method(ElementMatchers.named("invoke"))
                    .intercept(MethodCall.invoke(constructor).withArgumentArrayElements(0))
                    .make()
                    .load(declaringClass.getClassLoader())
                    .getLoaded();

            return (ConstructorInvoker) invokerClass.getDeclaredConstructor().newInstance();
        } catch (Throwable t) {
            constructor.setAccessible(true);
            return args -> constructor.newInstance(args);
        }
    }

    private static Class<?> generateByteBuddyImplementation(Class<?> abstractOrInterfaceClass) {
        try {
            Method[] abstractMethods = Arrays.stream(abstractOrInterfaceClass.getMethods())
                    .filter(m -> Modifier.isAbstract(m.getModifiers()) && m.getParameterCount() == 0 && m.getReturnType() != void.class)
                    .toArray(Method[]::new);

            if (abstractMethods.length == 0) {
                throw new IllegalArgumentException("No abstract getter methods found on " + abstractOrInterfaceClass);
            }

            Constructor<?> superConstructor = abstractOrInterfaceClass.isInterface()
                    ? Object.class.getDeclaredConstructor()
                    : abstractOrInterfaceClass.getDeclaredConstructor();

            Class<?>[] paramTypes = Arrays.stream(abstractMethods).map(Method::getReturnType).toArray(Class<?>[]::new);

            Implementation.Composable constructorCall = MethodCall.invoke(superConstructor);
            for (int i = 0; i < abstractMethods.length; i++) {
                constructorCall = constructorCall.andThen(FieldAccessor.ofField(abstractMethods[i].getName()).setsArgumentAt(i));
            }

            var builder = new ByteBuddy()
                    .subclass(abstractOrInterfaceClass, ConstructorStrategy.Default.NO_CONSTRUCTORS)
                    .name(abstractOrInterfaceClass.getName() + "$ByteBuddyDerivedImpl");

            for (Method method : abstractMethods) {
                builder = builder.defineField(method.getName(), method.getReturnType(), Visibility.PRIVATE)
                        .method(ElementMatchers.is(method))
                        .intercept(FieldAccessor.ofField(method.getName()));
            }

            builder = builder.defineConstructor(Visibility.PUBLIC)
                    .withParameters(paramTypes)
                    .intercept(constructorCall);

            return builder.make().load(abstractOrInterfaceClass.getClassLoader()).getLoaded();
        } catch (Throwable t) {
            throw new IllegalArgumentException("Cannot generate implementation for abstract/interface: " + abstractOrInterfaceClass, t);
        }
    }
}
