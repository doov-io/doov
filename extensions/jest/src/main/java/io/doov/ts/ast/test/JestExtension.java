/*
 * Copyright (C) by Courtanet, All Rights Reserved.
 */
package io.doov.ts.ast.test;

import static io.doov.core.dsl.meta.i18n.ResourceBundleProvider.BUNDLE;
import static io.doov.ts.ast.test.JestTemplate.toTemplateParameters;
import static io.doov.tsparser.util.TypeScriptParserFactory.parseUsing;
import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.*;
import java.time.LocalDate;
import java.util.Locale;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.ParseTreeWalker;
import org.junit.jupiter.api.extension.*;

import io.doov.assertions.ts.TypeScriptAssertionContext;
import io.doov.core.dsl.lang.Context;
import io.doov.core.dsl.lang.Result;
import io.doov.ts.ast.AstTSRenderer;
import io.doov.ts.ast.writer.*;
import io.doov.tsparser.TypeScriptParser;

public class JestExtension implements BeforeAllCallback, AfterAllCallback, AfterEachCallback {

    private JestTestSpec jestTestSpec;
    private TypeScriptWriter writer;

    private Result result;
    private Context executionContext;

    @Override
    public void beforeAll(ExtensionContext context) {
        jestTestSpec = new JestTestSpec(context.getTestClass().get().getSimpleName());
    }

    @Override
    public void afterEach(ExtensionContext context) {
        String output = new String(((ByteArrayOutputStream) writer.getOutput()).toByteArray());
        TestCaseSpec testCaseSpec = new TestCaseSpec(context.getTestMethod().get().getName(),
                new RuleAssertionSpec("DOOV.when(" + output + ").validate()", String.valueOf(result.value())));
        testCaseSpec.getFieldAssertions().addAll(
                writer.getFields().stream()
                        .map(f -> new FieldAssertionSpec(f.field(), f.name(), getExpectedValue(f)))
                        .collect(Collectors.toList())
        );
        jestTestSpec.getTestCases().add(testCaseSpec);
        jestTestSpec.getImports().addAll(writer.getImports());
        jestTestSpec.getFields().addAll(writer.getFields());
    }

    private String getExpectedValue(FieldSpec f) {
        Object evalValue = executionContext.getEvalValue(f.field().id());
        if (evalValue != null) {
            Class<?> valueType = evalValue.getClass();
            if (valueType.isEnum()) {
                return valueType.getSimpleName() + "." + evalValue;
            } else if (String.class.equals(valueType)) {
                return "'" + evalValue + "'";
            } else if (LocalDate.class.equals(valueType)) {
                return "new Date('" + evalValue + "')";
            }
        }
        return String.valueOf(evalValue);
    }

    @Override
    public void afterAll(ExtensionContext context) {
        JestTemplate.writeToFile(toTemplateParameters(jestTestSpec), new File(jestTestSpec.getTestSuiteName() + ".test.ts"));
    }

    public JestTestSpec getJestTestSpec() {
        return jestTestSpec;
    }

    public String toTS(Result result) {
        this.result = result;
        return toTS(result.getContext());
    }

    public String toTS(Context context) {
        this.executionContext = result.getContext();
        final ByteArrayOutputStream ops = new ByteArrayOutputStream();
        writer = new DefaultTypeScriptWriter(Locale.US, ops, BUNDLE,
                field -> field.id().code().replace(" ", ""));
        new AstTSRenderer(writer, true).toTS(context.getRootMetadata());
        return new String(ops.toByteArray(), UTF_8);
    }

    public static TypeScriptAssertionContext parseAs(String ruleTs, Function<TypeScriptParser, ParseTree> contextGetter)
            throws IOException {
        TypeScriptAssertionContext context = parseUsing(ruleTs, TypeScriptAssertionContext::new);
        new ParseTreeWalker().walk(context, contextGetter.apply(context.getParser()));
        return context;
    }
}
