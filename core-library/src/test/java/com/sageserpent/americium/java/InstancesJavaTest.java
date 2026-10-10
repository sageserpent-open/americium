package com.sageserpent.americium.java;

import cyclops.control.Either;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.Instant;
import java.util.*;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;

public class InstancesJavaTest {
    private static final TrialsApi api = Trials.api();

    public enum Season {
        SPRING, SUMMER, AUTUMN, WINTER
    }

    public static class Person {
        private final String name;
        private final int age;

        public Person(String name, int age) {
            this.name = name;
            this.age = age;
        }

        public String name() { return name; }
        public int age() { return age; }
    }

    public static class Shape {
        private final String type;
        private final double area;

        public Shape(String type, double area) {
            this.type = type;
            this.area = area;
        }

        public Shape(double area) {
            this("unknown", area);
        }

        public String type() { return type; }
        public double area() { return area; }
    }

    public record Point(int x, int y) {}

    public static class Box<T> {
        private final T content;

        public Box(T content) {
            this.content = content;
        }

        public T content() { return content; }
    }

    public static class BinaryTree {
        private final int value;
        private final BinaryTree left;
        private final BinaryTree right;

        public BinaryTree(int value, BinaryTree left, BinaryTree right) {
            this.value = value;
            this.left = left;
            this.right = right;
        }

        public BinaryTree(int value) {
            this(value, null, null);
        }

        public int value() { return value; }
        public BinaryTree left() { return left; }
        public BinaryTree right() { return right; }
    }

    public interface Student {
        String name();
        int grade();
    }

    @Test
    void testPrimitivesAndStandardTypes() {
        api.instances(int.class).withLimit(10).supplyTo(val -> assertThat(val, is(notNullValue())));
        api.instances(Integer.class).withLimit(10).supplyTo(val -> assertThat(val, is(notNullValue())));
        api.instances(String.class).withLimit(10).supplyTo(val -> assertThat(val, is(notNullValue())));
        api.instances(Boolean.class).withLimit(10).supplyTo(val -> assertThat(val, is(notNullValue())));
        api.instances(Double.class).withLimit(10).supplyTo(val -> assertThat(val, is(notNullValue())));
        api.instances(Instant.class).withLimit(10).supplyTo(val -> assertThat(val, is(notNullValue())));
        api.instances(BigInteger.class).withLimit(10).supplyTo(val -> assertThat(val, is(notNullValue())));
        api.instances(BigDecimal.class).withLimit(10).supplyTo(val -> assertThat(val, is(notNullValue())));
    }

    @Test
    void testEnumDerivation() {
        Set<Season> seasonsSeen = new HashSet<>();
        api.instances(Season.class).withLimit(20).supplyTo(seasonsSeen::add);
        assertThat(seasonsSeen.size(), greaterThanOrEqualTo(1));
    }

    @Test
    void testClassWithSingleConstructor() {
        api.instances(Person.class).withLimit(20).supplyTo(person -> {
            assertThat(person.name(), is(notNullValue()));
            assertThat(person.age(), is(notNullValue()));
        });
    }

    @Test
    void testClassWithMultipleConstructors() {
        Set<String> shapeTypesSeen = new HashSet<>();
        api.instances(Shape.class).withLimit(50).supplyTo(shape -> {
            assertThat(shape.area(), is(notNullValue()));
            shapeTypesSeen.add(shape.type());
        });
        assertThat(shapeTypesSeen.contains("unknown"), is(true));
    }

    @Test
    void testRecordDerivation() {
        api.instances(Point.class).withLimit(20).supplyTo(point -> {
            assertThat(point.x(), is(notNullValue()));
            assertThat(point.y(), is(notNullValue()));
        });
    }

    @Test
    void testGenericClassDerivation() {
        Trials<Box<String>> stringBoxTrials = api.instances(new com.google.common.reflect.TypeToken<Box<String>>() {}.getType());
        stringBoxTrials.withLimit(20).supplyTo(box -> {
            assertThat(box, is(notNullValue()));
            assertThat(box.content(), is(notNullValue()));
        });
    }

    @Test
    void testRecursiveClassDerivation() {
        api.instances(BinaryTree.class).withLimit(30).supplyTo(tree -> {
            assertThat(tree, is(notNullValue()));
        });
    }

    @Test
    void testInterfaceDerivation() {
        api.instances(Student.class).withLimit(20).supplyTo(student -> {
            assertThat(student, is(notNullValue()));
            assertThat(student.name(), is(notNullValue()));
        });
    }
}
