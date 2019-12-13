/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package org.codehaus.groovy.ast;

import org.codehaus.groovy.ast.tools.GenericsUtils;
import org.codehaus.groovy.ast.tools.WideningCategories;

import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * This class is used to describe generic type signatures for ClassNodes.
 *
 * @see ClassNode
 */
public class GenericsType extends ASTNode {
    public static final GenericsType[] EMPTY_ARRAY = new GenericsType[0];

    private String name;
    private ClassNode type;
    // GRECLIPSE edit
    private /*final*/ ClassNode lowerBound;
    private /*final*/ ClassNode[] upperBounds;
    // GRECLIPSE end
    private boolean placeholder, resolved, wildcard;

    public GenericsType(final ClassNode type, final ClassNode[] upperBounds, final ClassNode lowerBound) {
        setType(type);
        this.lowerBound = lowerBound;
        this.upperBounds = upperBounds;
        this.placeholder = type.isGenericsPlaceHolder();
        setName(placeholder ? type.getUnresolvedName() : type.getName());
    }

    public GenericsType(final ClassNode basicType) {
        this(basicType, null, null);
    }

    // GRECLIPSE add
    public GenericsType() {
    }
    // GRECLIPSE end

    public ClassNode getType() {
        return type;
    }

    public void setType(final ClassNode type) {
        this.type = Objects.requireNonNull(type);
    }

    public String toString() {
        return toString(this, new HashSet<>());
    }

    private static String toString(final GenericsType gt, final Set<String> visited) {
        ClassNode type = gt.getType();
        boolean wildcard = gt.isWildcard();
        boolean placeholder = gt.isPlaceholder();
        ClassNode lowerBound = gt.getLowerBound();
        ClassNode[] upperBounds = gt.getUpperBounds();

        if (placeholder) visited.add(gt.getName());

        StringBuilder ret = new StringBuilder(wildcard || placeholder ? gt.getName() : genericsBounds(type, visited));
        if (lowerBound != null) {
            ret.append(" super ").append(genericsBounds(lowerBound, visited));
        } else if (upperBounds != null
                // T extends Object should just be printed as T
                && !(placeholder && upperBounds.length == 1 && !upperBounds[0].isGenericsPlaceHolder() && upperBounds[0].getName().equals("java.lang.Object"))) {
            ret.append(" extends ");
            for (int i = 0, n = upperBounds.length; i < n; i += 1) {
                if (i != 0) ret.append(" & ");
                ret.append(genericsBounds(upperBounds[i], visited));
            }
        }
        return ret.toString();
    }

    private static String nameOf(final ClassNode theType) {
        StringBuilder ret = new StringBuilder();
        if (theType.isArray()) {
            ret.append(nameOf(theType.getComponentType()));
            ret.append("[]");
        } else {
            ret.append(theType.getName());
        }
        return ret.toString();
    }

    private static String genericsBounds(final ClassNode theType, final Set<String> visited) {
        StringBuilder ret = new StringBuilder();

        if (theType.isArray()) {
            ret.append(nameOf(theType));
        } else if (theType.getOuterClass() != null) {
            String parentClassNodeName = theType.getOuterClass().getName();
            if (Modifier.isStatic(theType.getModifiers()) || theType.isInterface()) {
                ret.append(parentClassNodeName);
            } else {
                ret.append(genericsBounds(theType.getOuterClass(), new HashSet<>()));
            }
            ret.append('.');
            ret.append(theType.getName().substring(parentClassNodeName.length() + 1));
        } else {
            ret.append(theType.getName());
        }

        GenericsType[] genericsTypes = theType.getGenericsTypes();
        if (genericsTypes == null || genericsTypes.length == 0) {
            return ret.toString();
        }

        // TODO: instead of catching Object<T> here stop it from being placed into type in first place
        if (genericsTypes.length == 1 && genericsTypes[0].isPlaceholder() && theType.getName().equals("java.lang.Object")) {
            return genericsTypes[0].getName();
        }

        ret.append('<');
        for (int i = 0, n = genericsTypes.length; i < n; i += 1) {
            if (i != 0) ret.append(", ");

            GenericsType type = genericsTypes[i];
            if (type.isPlaceholder() && visited.contains(type.getName())) {
                ret.append(type.getName());
            } else {
                ret.append(toString(type, visited));
            }
        }
        ret.append('>');

        return ret.toString();
    }

    public String getName() {
        return (isWildcard() ? "?" : name);
    }

    public void setName(final String name) {
        this.name = Objects.requireNonNull(name);
    }

    public boolean isResolved() {
        return (resolved || isPlaceholder());
    }

    public void setResolved(final boolean resolved) {
        this.resolved = resolved;
    }

    public boolean isPlaceholder() {
        return placeholder;
    }

    public void setPlaceholder(final boolean placeholder) {
        this.placeholder = placeholder;
        getType().setGenericsPlaceHolder(placeholder);
    }

    public boolean isWildcard() {
        return wildcard;
    }

    public void setWildcard(final boolean wildcard) {
        this.wildcard = wildcard;
    }

    public ClassNode getLowerBound() {
        return lowerBound;
    }

    public ClassNode[] getUpperBounds() {
        return upperBounds;
    }

    // GRECLIPSE add
    public void setLowerBound(final ClassNode bound) {
        this.lowerBound = bound;
    }
    public void setUpperBounds(final ClassNode[] bounds) {
        this.upperBounds = bounds;
    }
    public void setPlaceHolder(final boolean placeholder) {
        this.placeholder = placeholder;
    }
    // GRECLIPSE end

    /**
     * If you have a class which extends a class using generics, returns the superclass with parameterized types. For
     * example, if you have:
     * <code>class MyList&lt;T&gt; extends LinkedList&lt;T&gt;
     * def list = new MyList&lt;String&gt;
     * </code>
     * then the parameterized superclass for MyList&lt;String&gt; is LinkedList&lt;String&gt;
     * @param classNode the class for which we want to return the parameterized superclass
     * @return the parameterized superclass
     */
    private static ClassNode getParameterizedSuperClass(final ClassNode classNode) {
        if (ClassHelper.OBJECT_TYPE.equals(classNode)) return null;
        ClassNode superClass = classNode.getUnresolvedSuperClass();
        if (superClass == null) return ClassHelper.OBJECT_TYPE;

        if (!classNode.isUsingGenerics() || !superClass.isUsingGenerics()) {
            return superClass;
        }

        GenericsType[] genericsTypes = classNode.getGenericsTypes();
        GenericsType[] redirectGenericTypes = classNode.redirect().getGenericsTypes();
        superClass = superClass.getPlainNodeReference();
        if (genericsTypes == null || redirectGenericTypes == null || superClass.getGenericsTypes() == null) {
            return superClass;
        }
        for (int i = 0, genericsTypesLength = genericsTypes.length; i < genericsTypesLength; i += 1) {
            if (redirectGenericTypes[i].isPlaceholder()) {
                GenericsType genericsType = genericsTypes[i];
                GenericsType[] superGenericTypes = superClass.getGenericsTypes();
                for (int j = 0, superGenericTypesLength = superGenericTypes.length; j < superGenericTypesLength; j += 1) {
                    final GenericsType superGenericType = superGenericTypes[j];
                    if (superGenericType.isPlaceholder() && superGenericType.getName().equals(redirectGenericTypes[i].getName())) {
                        superGenericTypes[j] = genericsType;
                    }
                }
            }
        }
        return superClass;
    }

    /**
     * Tells if the provided class node is compatible with this generic type definition
     * @param classNode the class node to be checked
     * @return true if the class node is compatible with this generics type definition
     */
    public boolean isCompatibleWith(final ClassNode classNode) {
        return new GenericsTypeMatcher().matches(classNode);
    }

    /**
     * Implements generics type comparison.
     */
    private class GenericsTypeMatcher {

        public boolean implementsInterfaceOrIsSubclassOf(final ClassNode type, final ClassNode superOrInterface) {
            boolean result = type.equals(superOrInterface)
                    || type.isDerivedFrom(superOrInterface)
                    || type.implementsInterface(superOrInterface);
            if (result) {
                return true;
            }
            if (ClassHelper.GROOVY_OBJECT_TYPE.equals(superOrInterface) && type.getCompileUnit() != null) {
                // type is being compiled so it will implement GroovyObject later
                return true;
            }
            if (superOrInterface instanceof WideningCategories.LowestUpperBoundClassNode) {
                WideningCategories.LowestUpperBoundClassNode cn = (WideningCategories.LowestUpperBoundClassNode) superOrInterface;
                result = implementsInterfaceOrIsSubclassOf(type, cn.getSuperClass());
                if (result) {
                    for (ClassNode interfaceNode : cn.getInterfaces()) {
                        result = implementsInterfaceOrIsSubclassOf(type, interfaceNode);
                        if (!result) break;
                    }
                }
                if (result) return true;
            }
            if (type.isArray() && superOrInterface.isArray()) {
                return implementsInterfaceOrIsSubclassOf(type.getComponentType(), superOrInterface.getComponentType());
            }
            return false;
        }

        /**
         * Compares this generics type with the one represented by the provided class node. If the provided
         * classnode is compatible with the generics specification, returns true. Otherwise, returns false.
         * The check is complete, meaning that we also check "nested" generics.
         * @param classNode the classnode to be checked
         * @return true iff the classnode is compatible with this generics specification
         */
        public boolean matches(final ClassNode classNode) {
            GenericsType[] genericsTypes = classNode.getGenericsTypes();
            if (genericsTypes != null && genericsTypes.length == 0) {
                return true; // diamond always matches
            }
            if (classNode.isGenericsPlaceHolder()) {
                // if the classnode we compare to is a generics placeholder (like <E>) then we
                // only need to check that the names are equal
                if (genericsTypes == null) {
                    return true;
                }
                if (isWildcard()) {
                    if (getLowerBound() != null) {
                        ClassNode lowerBound = getLowerBound();
                        return genericsTypes[0].name.equals(lowerBound.getUnresolvedName());
                    }
                    if (getUpperBounds() != null) {
                        for (ClassNode upperBound : getUpperBounds()) {
                            if (genericsTypes[0].name.equals(upperBound.getUnresolvedName())) {
                                return true;
                            }
                        }
                        return false;
                    }
                }
                return genericsTypes[0].name.equals(name);
            }
            if (isWildcard() || isPlaceholder()) {
                ClassNode lowerBound = getLowerBound();
                ClassNode[] upperBounds = getUpperBounds();
                // if the current generics spec is a wildcard spec or a placeholder spec
                // then we must check lower and upper bounds
                if (lowerBound != null) {
                    // if a lower bound is declared, then we must perform the same checks that for an upper bound
                    // but with reversed arguments
                    if (!implementsInterfaceOrIsSubclassOf(lowerBound, classNode)) {
                        return false;
                    }
                    return checkGenerics(classNode);
                }
                if (upperBounds != null) {
                    // check that the provided classnode is a subclass of all provided upper bounds
                    for (ClassNode upperBound : upperBounds) {
                        if (!implementsInterfaceOrIsSubclassOf(classNode, upperBound)) {
                            return false;
                        }
                    }
                    // if the provided classnode is a subclass of the upper bound
                    // then check that the generic types supplied by the class node are compatible with
                    // this generics specification
                    // for example, we could have the spec saying List<String> but provided classnode
                    // saying List<Integer>
                    return checkGenerics(classNode);
                }
                // If there are no bounds, the generic type is basically Object, and everything is compatible.
                return true;
            }
            // last, we could have the spec saying List<String> and a classnode saying List<Integer> so
            // we must check that generics are compatible.
            // The null check is normally not required but done to prevent from NPEs
            return getType().equals(classNode) && compareGenericsWithBound(classNode, type);
        }

        /**
         * Iterates over each generics bound of this generics specification, and checks
         * that the generics defined by the bound are compatible with the generics specified
         * by the type.
         * @param classNode the classnode the bounds should be compared with
         * @return true if generics from bounds are compatible
         */
        private boolean checkGenerics(final ClassNode classNode) {
            ClassNode lowerBound = getLowerBound();
            ClassNode[] upperBounds = getUpperBounds();
            if (lowerBound != null) {
                if (!lowerBound.redirect().isUsingGenerics()) {
                    return compareGenericsWithBound(classNode, lowerBound);
                }
            }
            if (upperBounds != null) {
                for (ClassNode upperBound : upperBounds) {
                    if (!compareGenericsWithBound(classNode, upperBound)) {
                        return false;
                    }
                }
            }
            return true;
        }

        /**
         * Given a parameterized type (List&lt;String&gt; for example), checks that its
         * generic types are compatible with those from a bound.
         * @param classNode the classnode from which we will compare generics types
         * @param bound the bound to which the types will be compared
         * @return true if generics are compatible
         */
        private boolean compareGenericsWithBound(final ClassNode classNode, final ClassNode bound) {
            if (classNode == null) {
                return false;
            }
            if (!bound.isUsingGenerics() || (classNode.getGenericsTypes() == null && classNode.redirect().getGenericsTypes() != null)) {
                // if the bound is not using generics, there's nothing to compare with
                return true;
            }
            if (!classNode.equals(bound)) {
                 // the class nodes are on different types
                // in this situation, we must choose the correct execution path : either the bound
                // is an interface and we must find the implementing interface from the classnode
                // to compare their parameterized generics, or the bound is a regular class and we
                // must compare the bound with a superclass
                if (bound.isInterface()) {
                    Set<ClassNode> interfaces = classNode.getAllInterfaces();
                    // iterate over all interfaces to check if any corresponds to the bound we are
                    // comparing to
                    for (ClassNode anInterface : interfaces) {
                        if (anInterface.equals(bound)) {
                            // when we obtain an interface, the types represented by the interface
                            // class node are not parameterized. This means that we must create a
                            // new class node with the parameterized types that the current class node
                            // has defined.
                            ClassNode node = GenericsUtils.parameterizeType(classNode, anInterface);
                            return compareGenericsWithBound(node, bound);
                        }
                    }
                }
                if (bound instanceof WideningCategories.LowestUpperBoundClassNode) {
                    // another special case here, where the bound is a "virtual" type
                    // we must then check the superclass and the interfaces
                    boolean success = compareGenericsWithBound(classNode, bound.getSuperClass());
                    if (success) {
                        ClassNode[] interfaces = bound.getInterfaces();
                        for (ClassNode anInterface : interfaces) {
                            success &= compareGenericsWithBound(classNode, anInterface);
                            if (!success) break;
                        }
                        if (success) return true;
                    }
                }
                return compareGenericsWithBound(getParameterizedSuperClass(classNode), bound);
            }
            GenericsType[] cnTypes = classNode.getGenericsTypes();
            if (cnTypes == null && classNode.isRedirectNode()) {
                cnTypes = classNode.redirect().getGenericsTypes();
            }
            if (cnTypes == null) {
                // may happen if generic type is Foo<T extends Foo> and classnode is Foo -> Foo
                return true;
            }
            GenericsType[] redirectBoundGenericTypes = bound.redirect().getGenericsTypes();
            Map<GenericsTypeName, GenericsType> boundPlaceHolders = GenericsUtils.extractPlaceholders(bound);
            Map<GenericsTypeName, GenericsType> classNodePlaceholders = GenericsUtils.extractPlaceholders(classNode);
            boolean match = true;
            for (int i = 0; redirectBoundGenericTypes != null && i < redirectBoundGenericTypes.length && match; i += 1) {
                GenericsType redirectBoundType = redirectBoundGenericTypes[i];
                GenericsType classNodeType = cnTypes[i];
                if (classNodeType.isPlaceholder()) {
                    GenericsTypeName name = new GenericsTypeName(classNodeType.getName());
                    if (redirectBoundType.isPlaceholder()) {
                        GenericsTypeName gtn = new GenericsTypeName(redirectBoundType.getName());
                        match = name.equals(gtn);
                        if (!match) {
                            GenericsType genericsType = boundPlaceHolders.get(gtn);
                            match = false;
                            if (genericsType != null) {
                                if (genericsType.isPlaceholder()) {
                                    match = true;
                                } else if (genericsType.isWildcard()) {
                                    if (genericsType.getUpperBounds() != null) {
                                        for (ClassNode ub : genericsType.getUpperBounds()) {
                                            match |= redirectBoundType.isCompatibleWith(ub);
                                        }
                                        if (genericsType.getLowerBound() != null) {
                                            match |= redirectBoundType.isCompatibleWith(genericsType.getLowerBound());
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        if (classNodePlaceholders.containsKey(name))
                            classNodeType = classNodePlaceholders.get(name);
                        match = classNodeType.isCompatibleWith(redirectBoundType.getType());
                    }
                } else {
                    if (redirectBoundType.isPlaceholder()) {
                        if (classNodeType.isPlaceholder()) {
                            match = classNodeType.getName().equals(redirectBoundType.getName());
                        } else {
                            GenericsTypeName name = new GenericsTypeName(redirectBoundType.getName());
                            if (boundPlaceHolders.containsKey(name)) {
                                redirectBoundType = boundPlaceHolders.get(name);
                                boolean wildcard = redirectBoundType.isWildcard();
                                boolean placeholder = redirectBoundType.isPlaceholder();
                                if (placeholder || wildcard) {
                                    // placeholder aliases, like Map<U,V> -> Map<K,V>
                                    if (wildcard) {
                                        // ex: Comparable<Integer> <=> Comparable<? super T>
                                        if (redirectBoundType.getLowerBound() != null) {
                                            GenericsType gt = new GenericsType(redirectBoundType.getLowerBound());
                                            if (gt.isPlaceholder()) {
                                                // check for recursive generic typedef, like in
                                                // <T extends Comparable<? super T>>
                                                GenericsTypeName gtn = new GenericsTypeName(gt.getName());
                                                if (classNodePlaceholders.containsKey(gtn)) {
                                                    gt = classNodePlaceholders.get(gtn);
                                                }
                                            }
                                            match = implementsInterfaceOrIsSubclassOf(gt.getType(), classNodeType.getType());
                                        }
                                        if (match && redirectBoundType.getUpperBounds() != null) {
                                            for (ClassNode upperBound : redirectBoundType.getUpperBounds()) {
                                                GenericsType gt = new GenericsType(upperBound);
                                                if (gt.isPlaceholder()) {
                                                    // check for recursive generic typedef, like in
                                                    // <T extends Comparable<? super T>>
                                                    GenericsTypeName gtn = new GenericsTypeName(gt.getName());
                                                    if (classNodePlaceholders.containsKey(gtn)) {
                                                        gt = classNodePlaceholders.get(gtn);
                                                    }
                                                }
                                                match = implementsInterfaceOrIsSubclassOf(classNodeType.getType(), gt.getType())
                                                         || classNodeType.isCompatibleWith(gt.getType()); // workaround for GROOVY-6095
                                                if (!match) break;
                                            }
                                        }
                                        return match;
                                    } else if (classNodePlaceholders.containsKey(name)) {
                                        redirectBoundType = classNodePlaceholders.get(name);
                                    }
                                }
                            }
                            match = redirectBoundType.isCompatibleWith(classNodeType.getType());
                        }
                    } else {
                        // TODO: the check for isWildcard should be replaced with a more complete check
                        match = redirectBoundType.isWildcard() || classNodeType.isCompatibleWith(redirectBoundType.getType());
                    }
                }
            }
            return match;
        }
    }

    /**
     * Represents GenericsType name
     * TODO In order to distinguish GenericsType with same name(See GROOVY-8409), we should add a property to keep the declaring class.
     *
     * fixing GROOVY-8409 steps:
     * 1) change the signature of constructor GenericsTypeName to `GenericsTypeName(String name, ClassNode declaringClass)`
     * 2) try to fix all compilation errors(if `GenericsType` has declaringClass property, the step would be a bit easy to fix...)
     * 3) run all tests to see whether the change breaks anything
     * 4) if all tests pass, congratulations! but if some tests are broken, try to debug and find why...
     *
     * We should find a way to set declaring class for `GenericsType` first, it can be completed at the resolving phase.
     */
    public static class GenericsTypeName {
        private String name;

        public GenericsTypeName(final String name) {
            this.name = Objects.requireNonNull(name);
        }

        public String getName() {
            return name;
        }

        @Override
        public boolean equals(Object that) {
            if (this == that) return true;
            if (!(that instanceof GenericsTypeName)) return false;
            return getName().equals(((GenericsTypeName) that).getName());
        }

        @Override
        public int hashCode() {
            return getName().hashCode();
        }

        @Override
        public String toString() {
            return getName();
        }
    }
}
