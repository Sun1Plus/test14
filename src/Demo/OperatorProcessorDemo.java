package Demo;

import m2x.utils.TokenStream;
import org.jetbrains.annotations.Nullable;
import static m2x.utils.VarTypeHelper.*;
import java.io.*;
import java.util.*;

@SuppressWarnings("ConstantConditions")
public class OperatorProcessorDemo
{
    //用于存放changeable type关系
    private static final List<Relation> changeableRelations = new ArrayList<>();
    //用于存放fixed type关系
    private static final List<Relation> fixedRelations = new ArrayList<>();
    //用于存放vsip.h头文件中的内容
    private static final List<String> hFileContent = new ArrayList<>();

    //这里加载三个static成员的内容，之后都不再加载
    static
    {
        //将changeable_type.txt中的内容转换为Relation结构
        String filePath = "./input/changeable_type.txt";
        fillRelation(filePath, changeableRelations);

        //处理fixed type
        filePath = "./input/fixed_type.txt";
        fillRelation(filePath, fixedRelations);

        //读取vsip.h的内容并逐行保存在hFileContent中
        filePath = "./input/vsip.h";
        try
        {
            Scanner scanner = new Scanner(new File(filePath));
            while (scanner.hasNextLine())
            {
                hFileContent.add(scanner.nextLine().trim());
            }
        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();
        }
    }

    //static初始化块的辅助函数
    private static void fillRelation(String filePath, List<Relation> relations)
    {
        try
        {
            Scanner scanner = new Scanner(new File(filePath));
            while (scanner.hasNextLine())
            {
                String line = scanner.nextLine().trim();
                while (line.endsWith("|||"))
                {
                    //noinspection StringConcatenationInLoop
                    line += scanner.nextLine();
                }
                String[] strings = line.split("%");
                Relation relation = new Relation();
                relation.eigenOp = strings[0].trim();
                relation.template = strings[1].trim();
                //处理多个对应关系
                String[] ref = strings[2].trim().split("\\|\\|\\|");
                for (String s : ref)
                {
                    Relation.Reflection reflection = new Relation.Reflection();
                    String[] t = s.trim().split("=>");
                    TokenStream tokenStream = new TokenStream(t[0].trim());
                    tokenStream.getToken(); //(
                    String token = tokenStream.getToken();
                    while (!token.equals(")"))
                    {
                        if (!token.equals(","))
                        {
                            reflection.varType.add(token);
                        }
                        token = tokenStream.getToken();
                    }
                    reflection.cFunction = t[1].trim();
                    relation.reflections.add(reflection);
                }
                relations.add(relation);
            }
        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();
        }
    }

    /*
    * 处理simple type(= changeable type + fixed type)的算子。simple type定义：
    * 变量数量没变或者变少，并且没有变形。符合：
    * 1. 具有MATLAB函数调用、简单二元运算
    * 2. 固定形式（fixed type）
    * 3. 没有引起歧义的复合操作
    * 如果不是这种类型，返回null。
    * */
    @Nullable
    public String processSimpleType(String operator)
    {
        //分解identifier和op
        List<String> vars = new ArrayList<>();
        List<String> ops = new ArrayList<>();
        resolve(operator, vars, ops);

        //检查fixed type
        //检查fixed type的时候需要全部进行一遍检查
        //优先检查函数名
        for (Relation fr : fixedRelations)  //每次检查一个relation
        {
            if (Character.isLetter(fr.eigenOp.charAt(0)))
            {
                for (String v : vars)
                {
                    if (v.equals(fr.eigenOp))
                    {
                        //尝试在当前relation中寻找匹配的reflection
                        List<String> copyOfVars = new ArrayList<>(vars);
                        copyOfVars.remove(fr.eigenOp);
                        Relation.Reflection rf = matchFixedReflection(copyOfVars, ops, fr);
                        //如果match成功，输出结果
                        if (rf != null)
                        {
                            String cFunction = getCFunction(copyOfVars, fr, rf.cFunction);
                            //查询该函数是否存在，若否输出错误信息
                            String cFunctionHeader = cFunction.replaceAll("\\((\\S| )*\\)", "");    //这个正则表达式是匹配函数后面括号以及括号中的内容
                            if (!isValidCFunction(cFunctionHeader))
                            {
                                System.out.println(operator + "经过转换后得到未定义的C函数：" + cFunctionHeader);
                                System.exit(0);
                            }
                            return cFunction;
                        }
                        break;
                    }
                }
            }
        }

        //流程走到这里说明根据函数名没有找到匹配的，再检查op
        //取得op
        String op = getOp(new ArrayList<>(ops));
        if (op != null)
        {
            for (Relation fr : fixedRelations)
            {
                if (!Character.isLetter(fr.eigenOp.charAt(0)))
                {
                    if (op.equals(fr.eigenOp))
                    {
                        //尝试在当前relation中寻找匹配的reflection
                        Relation.Reflection rf = matchFixedReflection(vars, ops, fr);
                        //如果match成功，输出结果
                        if (rf != null)
                        {
                            String cFunction = getCFunction(vars, fr, rf.cFunction);
                            //查询该函数是否存在，若否输出错误信息
                            String cFunctionHeader = cFunction.replaceAll("\\((\\S| )*\\)", "");
                            if (!isValidCFunction(cFunctionHeader))
                            {
                                System.out.println(operator + "经过转换后得到未定义的C函数：" + cFunctionHeader);
                                System.exit(0);
                            }
                            return cFunction;
                        }
                        break;
                    }
                }
            }
        }

        //流程走到这里说明不是fixed type，再检查changeable type
        //检查是否存在对应的relation
        //检查是否是函数调用型
        Relation relation = null;               //标记在relations中匹配上的
        for (String s : vars)
        {
            for (Relation cr : changeableRelations)
            {
                if (cr.eigenOp.equals(s))
                {
                    relation = cr;
                    vars.remove(relation.eigenOp);
                    break;
                }
            }
        }

        //如果不是函数名，查找相应的op
        if (relation == null)
        {
            op = getOp(new ArrayList<>(ops));
            if (op != null)
            {
                for (Relation cr : changeableRelations)
                {
                    if (cr.eigenOp.equals(op))
                    {
                        relation = cr;
                        break;
                    }
                }
            }
        }

        if (relation == null)
        {
            return null;    //没有找到对应的relation
        }

        //找到对应的relation，根据变量和变量信息来确定C函数
        //首先考虑完全相等的情况
        List<String> type = getVarsType(vars);
        //todo 这里没有考虑常量参数
        for (Relation.Reflection rf : relation.reflections)
        {
            if(rf.varType.equals(type))
            {
                String cFunction = getCFunction(vars, relation, rf.cFunction);
                //查询该函数是否存在，若否输出错误信息
                String cFunctionHeader = cFunction.replaceAll("\\((\\S| )*\\)", "");
                if (!isValidCFunction(cFunctionHeader))
                {
                    System.out.println(operator + "经过转换后得到未定义的C函数：" + cFunctionHeader);
                    System.exit(0);
                }
                return cFunction;
            }
        }

        //再考虑是否是存在交换律的情况，
        //todo 现在只考虑了+ .*
        //因此变量数量要为3（结果 = op(两个参数)）
        if ((relation.eigenOp.equals("+") || relation.eigenOp.equals(".*"))
                && vars.size() == 3)
        {
            if (type.get(1).equals("s") || type.get(1).equals("cs")
                    || type.get(2).equals("s") || type.get(2).equals("cs"))
            {
                for (Relation.Reflection rf : relation.reflections)
                {
                    //交换type的位置
                    List<String> copyOfType = new ArrayList<>(type);
                    String t = copyOfType.get(1);
                    copyOfType.set(1, copyOfType.get(2));
                    copyOfType.set(2, t);
                    if (rf.varType.equals(copyOfType))
                    {
                        List<String> copyOfVars = new ArrayList<>(vars);
                        t = copyOfVars.get(1);
                        copyOfVars.set(1, copyOfVars.get(2));
                        copyOfVars.set(2, t);
                        String cFunction = getCFunction(copyOfVars, relation, rf.cFunction);
                        //查询该函数是否存在，若否输出错误信息
                        String cFunctionHeader = cFunction.replaceAll("\\((\\S| )*\\)", "");
                        if (!isValidCFunction(cFunctionHeader))
                        {
                            System.out.println(operator + "经过转换后得到未定义的C函数：" + cFunctionHeader);
                            System.exit(0);
                        }
                        return cFunction;
                    }
                }
            }
        }

        //流程走到这里说明转换失败，返回null并提升错误结果
        System.out.println(operator + "自动转换失败");
        return null;
    }

    //在做fixed type检查时寻找匹配的reflection
    @Nullable
    private Relation.Reflection matchFixedReflection(List<String> vars, List<String> ops, Relation relation)
    {
        List<String> varsOfTemplate = new ArrayList<>(), opsOfTemplate = new ArrayList<>();
        //分割template
        resolve(relation.template, varsOfTemplate, opsOfTemplate);
        varsOfTemplate.remove(relation.eigenOp);
        //先做op判断，要求op是完全相同的（虽然目前来看完全可以不用判断）
        if (!opsOfTemplate.equals(ops))
        {
            return null;
        }
        //然后遍历reflection，对比type和特殊类型位置上的参数
        List<String> type = getVarsType(vars);
        for (Relation.Reflection rf : relation.reflections)
        {
            if (type.equals(rf.varType))
            {
                //再查看特殊参数是否相等。虚数单位i == j
                for (int i = 0; i < type.size(); i++)
                {
                    if (type.get(i).equals("_"))
                    {
                        String var1 = varsOfTemplate.get(i), var2 = vars.get(i);
                        if ((var1.equals("i") || var1.equals("j"))
                                && (var2.equals("i") || var2.equals("j")))
                        {
                            continue;
                        }
                        else if (var1.equals(var2))
                        {
                            continue;
                        }
                        return null;
                    }
                }
                return rf;
            }
        }

        return null;
    }

    //分解变量和op
    private void resolve(String statement, List<String> vars, List<String> ops)
    {
        TokenStream tokenStream = new TokenStream(statement);
        while (tokenStream.nextToken() != null)
        {
            String token = tokenStream.getToken();
            char ch = token.charAt(0);
            //标识符添加到vars列表
            if (Character.isLetter(ch) || Character.isDigit(ch))
            {
                vars.add(token);  //这里把潜在的函数名也放进去了，之后再去掉
            }
            else if (ch != '(' && ch != ')')
            {
                ops.add(token);
            }
        }
    }

    //获取var列表中的每个var的type，放在列表中
    private List<String> getVarsType(List<String> vars)
    {
        List<String> type = new ArrayList<>();
        for (String v : vars)
        {
            if (Character.isDigit(v.charAt(0))
                    || (v.length() == 1 && (v.equals("i") || v.equals("j"))))
            {
                type.add("_");      //_用于特殊的参数的类型
            }
            else
            {
                type.add(getPostfix(v));
            }
        }
        return type;
    }

    //根据函数模板以及参数信息获得c函数调用并修饰
    private String getCFunction(List<String> vars, Relation relation, String cFunction)
    {
        List<String> varsOfTemplate = new ArrayList<>();
        List<String> opsOfTemplate = new ArrayList<>(); //在这里没有用到
        resolve(relation.template, varsOfTemplate, opsOfTemplate);
        varsOfTemplate.remove(relation.eigenOp);
        Map<String, String> map = new HashMap<>();
        for (int i = 0; i < vars.size(); i++)
        {
            map.put(varsOfTemplate.get(i), vars.get(i));
        }
        for (String k : varsOfTemplate)
        {
            String v = map.get(k);
            String postfix = getPostfix(v);

            if (postfix != null)
            {
                if (postfix.equals("cv") || postfix.equals("cm"))
                {
                    v = v.substring(0, v.length() - 3) + "CView";
                }
                else if(postfix.equals("v") || postfix.equals("m"))
                {
                    v = v.substring(0, v.length() - 2) + "View";
                }
                else
                {
                    v = v.replaceAll("_c?s", "");
                }
            }
            cFunction = cFunction.replaceAll("\\b" + k + "\\b", v);
        }
        String prefix = getPrefix(vars.get(0));
        switch (prefix)
        {
            case "int":
            case "int *":
            case "unsigned int":
            case "unsigned int *":
                cFunction = cFunction.replace("_p", "_i");
                break;
            case "float":
            case "float *":
                cFunction = cFunction.replace("_p", "_f");
                break;
            //todo _bl? copy的_i_i?
        }

        return cFunction;
    }

    // 判断一个函数是否在vsip.h中声明
    private boolean isValidCFunction(String function)
    {
        for (String s : hFileContent)
        {
            if (s.contains(function))
            {
                return true;
            }
        }
        return false;
    }

    //用于从op的列表中获取op，调用时注意参数会被修改
    @Nullable
    private String getOp(List<String> ops)
    {
        String op = null;
        if (ops.size() == 1 && ops.get(0).equals("="))  //特殊的copy
        {
            op = "=";
        }
        else if(ops.size() == 2)
        {
            ops.remove("=");
            if (ops.size() >= 2)
            {
                return null;
            }
            op = ops.get(0);
        }
        return op;
    }

    //保存MATLAB算子特征和对应C函数的关系
    private static class Relation
    {
        public String eigenOp;
        //MATLAB算子特征
        public String template;             //函数模板，即r = func(...)字符串
        public List<Reflection> reflections = new ArrayList<>();    //变量类型及对应的C语句

        private static class Reflection
        {
            public List<String> varType = new ArrayList<>();
            public String cFunction;
        }
    }

    public static void main(String[] args)
    {
        OperatorProcessorDemo or = new OperatorProcessorDemo();
        //Test...
        String operator1 = "pfSrcAmp_v = abs(pfSrcData_cv)";    //vsip_cvmag_f(pfSrcDataCView, pfSrcAmpView)
        String operator2 = "pfDst_v = 10 ^ pfSrc_v";    //vsip_vexp10_f(pfSrcView, pfDstView)
        String operator3 = "pfDst_cm = exp(i * pfSrc_m)";        //vsip_meuler_f(pfSrcView, pfDstCView)
        String operator4 = "pfDst_v = pfSrcA_s + pfSrcB_v";     //vsip_svadd_f(pfSrcA, pfSrcBView, pfDstView)
        String operator5 = "pfDst_cv = pfSrcA_cs + pfSrcB_cv";      //vsip_csvadd_f(pfSrcA, pfSrcBCView, pfDstCView)
        String operator6 = "pfDst_cv = pfSrcA_cv + pfSrcB_cs";      //vsip_csvadd_f(pfSrcB, pfSrcACView, pfDstCView)

        String cf = or.processSimpleType(operator6);
        System.out.println(cf);
    }
}

