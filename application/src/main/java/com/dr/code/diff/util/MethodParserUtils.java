package com.dr.code.diff.util;

import cn.hutool.crypto.SecureUtil;
import com.dr.code.diff.dto.MethodInfoResult;
import com.dr.common.errorcode.BizCode;
import com.dr.common.exception.BizException;
import com.dr.common.log.LoggerUtil;
import com.github.javaparser.JavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.NodeList;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.expr.SimpleName;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.resolution.MethodUsage;
import com.github.javaparser.resolution.declarations.ResolvedMethodDeclaration;
import com.github.javaparser.symbolsolver.JavaSymbolSolver;
import com.github.javaparser.symbolsolver.javaparsermodel.JavaParserFacade;
import com.github.javaparser.symbolsolver.resolution.typesolvers.CombinedTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.JavaParserTypeSolver;
import com.github.javaparser.symbolsolver.resolution.typesolvers.ReflectionTypeSolver;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Resource;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

/**
 * @ProjectName: base-service
 * @Package: com.dr.codediff.util
 * @Description: 解析获取类的方法
 * @Author: duanrui
 * @CreateDate: 2021/1/8 21:06
 * @Version: 1.0
 * <p>
 * Copyright: Copyright (c) 2021
 */
@Slf4j
public class MethodParserUtils {

    /**
     * 解析类获取类的所有方法
     *
     * @param classFile
     * @return
     */
    public static List<MethodInfoResult> parseMethods(String classFile) {
        List<MethodInfoResult> list = new ArrayList<>();
        try (FileInputStream in = new FileInputStream(classFile)) {
            JavaParser javaParser = new JavaParser();
            CombinedTypeSolver combinedTypeSolver = new CombinedTypeSolver();
            String sourcePath = classFile.split("src/main/java/")[0];
            combinedTypeSolver.add(new JavaParserTypeSolver(new File(sourcePath)));
            combinedTypeSolver.add(new ReflectionTypeSolver());
            JavaSymbolSolver symbolSolver = new JavaSymbolSolver(combinedTypeSolver);
            javaParser.getParserConfiguration().setSymbolResolver(symbolSolver);
            CompilationUnit cu = javaParser.parse(in).getResult().orElseThrow(() -> new BizException(BizCode.PARSE_JAVA_FILE));
            //由于jacoco不会统计接口覆盖率，没比较计算接口的方法，此处排除接口类
            final List<?> types = cu.getTypes();
            boolean isInterface = types.stream().filter(t -> t instanceof ClassOrInterfaceDeclaration).anyMatch(t -> ((ClassOrInterfaceDeclaration) t).isInterface());
            if (isInterface) {
                return list;
            }
            cu.accept(new MethodVisitor(), list);
            return list;
        } catch (IOException e) {
            LoggerUtil.error(log, "读取class类失败", e);
            throw new BizException(BizCode.PARSE_JAVA_FILE);
        }

    }

    /**
     * javaparser工具类核心方法，主要通过这个类遍历class文件的方法，此方法主要是获取出代码的所有方法，然后再去对比方法是否存在差异
     */
    private static class MethodVisitor extends VoidVisitorAdapter<List<MethodInfoResult>> {
        @Override
        public void visit(MethodDeclaration m, List<MethodInfoResult> list) {
            //删除注释
            m.removeComment();
            //计算方法体的hash值，疑问，空格，特殊转义字符会影响结果，导致相同匹配为差异？建议提交代码时统一工具格式化
            String md5 = SecureUtil.md5(m.toString());
            NodeList<Parameter> parameters = m.getParameters();
            List<String> params = parameters.stream().map(e -> {
                if (e.getType().isClassOrInterfaceType()) {
                    return e.getType().asClassOrInterfaceType().getNameAsString();
                }
                return e.getType().toString().trim();

            }).collect(Collectors.toList());

            MethodInfoResult result = MethodInfoResult.builder()
                    .md5(md5)
                    .methodName(m.getNameAsString())
                    .parameters(params)
                    .build();
            list.add(result);
            super.visit(m, list);
        }

    }
}
