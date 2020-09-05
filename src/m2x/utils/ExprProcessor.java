package m2x.utils;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import org.dom4j.*;
import org.dom4j.io.*;

@SuppressWarnings("ConstantConditions")
public class ExprProcessor
{

    private static List<String> functionName;

    public static Element getExprElement(String expr)
    {
        String filePath = "./input/function_names.txt";
        getFunctionName(filePath);
        TokenStream tokenStream = new TokenStream(expr);
        return assignmentExpr(tokenStream);
    }

    private static void getFunctionName(String filePath)
    {
        Scanner scanner = null;
        try
        {
            scanner = new Scanner(new File(filePath));
        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();
        }

        functionName = new ArrayList<>();
        //noinspection ConstantConditions
        while (scanner.hasNextLine())
        {
            functionName.add(scanner.nextLine());
        }
    }

    private static boolean isFunctionName(String identifier)
    {
        if (functionName.isEmpty())
        {
            return false;
        }
        for (String str : functionName)
        {
            if (identifier.equals(str))
            {
                return true;
            }
        }
        return false;
    }

    private static Element assignmentExpr(TokenStream tokenStream)
    {
        Element lLeft = logicalOrExpr(tokenStream);
        Element lRight;
        String token = tokenStream.nextToken();
        if (token != null && token.equals("="))
        {
            tokenStream.getToken();
            //先判断是否是冒号表达式
            TokenStream newStream = new TokenStream(tokenStream.getLine());
            newStream.getToken();
            String nextToken = newStream.nextToken();
            if (nextToken != null && nextToken.equals(":"))
            {
                lRight = colonExpr(tokenStream);
            }
            else
            {
                lRight = logicalOrExpr(tokenStream);
            }
            Element a = DocumentHelper.createElement("AssignmentExpr")
                    .addAttribute("operator", "=");
            a.add(lLeft);
            a.add(lRight);
            lLeft = a;
        }
        return lLeft;
    }

    private static Element logicalOrExpr(TokenStream tokenStream)
    {
        Element lLeft = logicalAndExpr(tokenStream);
        String token = tokenStream.nextToken();
        while (token != null && token.equals("||"))
        {
            tokenStream.getToken();
            Element lRight = logicalAndExpr(tokenStream);
            Element l = DocumentHelper.createElement("LogicalOrExpr")
                    .addAttribute("operator", token);
            l.add(lLeft);
            l.add(lRight);
            lLeft = l;
            token = tokenStream.nextToken();
        }

        return lLeft;
    }

    private static Element logicalAndExpr(TokenStream tokenStream)
    {
        Element eLeft = equalityExpr(tokenStream);
        String token = tokenStream.nextToken();
        while (token != null && token.equals("&&"))
        {
            tokenStream.getToken();
            Element eRight = equalityExpr(tokenStream);
            Element l = DocumentHelper.createElement("LogicalAndExpr")
                    .addAttribute("operator", token);
            l.add(eLeft);
            l.add(eRight);
            eLeft = l;
            token = tokenStream.nextToken();
        }

        return eLeft;
    }

    private static Element equalityExpr(TokenStream tokenStream)
    {
        Element rLeft = relationalExpr(tokenStream);
        String token = tokenStream.nextToken();
        while (token != null && (token.equals("==") || token.equals("~=")))
        {
            tokenStream.getToken();
            Element rRight = relationalExpr(tokenStream);
            Element e = DocumentHelper.createElement("EqualityExpr")
                    .addAttribute("operator", token);
            e.add(rLeft);
            e.add(rRight);
            rLeft = e;
            token = tokenStream.nextToken();
        }

        return rLeft;
    }

    private static Element relationalExpr(TokenStream tokenStream)
    {
        Element aLeft = additiveExpr(tokenStream);
        String token = tokenStream.nextToken();
        while (token != null && (token.equals("<") || token.equals("<=") || token.equals(">") || token.equals(">=")))
        {
            tokenStream.getToken();
            Element aRight = additiveExpr(tokenStream);
            Element r = DocumentHelper.createElement("RelationalExpr")
                    .addAttribute("operator", token);
            r.add(aLeft);
            r.add(aRight);
            aLeft = r;
            token = tokenStream.nextToken();
        }

        return aLeft;
    }

    private static Element additiveExpr(TokenStream tokenStream)
    {
        Element mLeft = multiplicativeExpr(tokenStream);
        String token = tokenStream.nextToken();
        while (token != null && (token.equals("+") || token.equals("-")))
        {
            tokenStream.getToken();
            Element mRight = multiplicativeExpr(tokenStream);
            Element a = DocumentHelper.createElement("AdditiveExpr")
                    .addAttribute("operator", token);
            a.add(mLeft);
            a.add(mRight);
            mLeft = a;
            token = tokenStream.nextToken();
        }

        return mLeft;
    }

    private static Element multiplicativeExpr(TokenStream tokenStream)
    {
        Element uLeft = unaryExpr(tokenStream);
        String token = tokenStream.nextToken();
        while (token != null && (token.equals("*") || token.equals("/")))
        {
            tokenStream.getToken();
            Element uRight = unaryExpr(tokenStream);
            Element m = DocumentHelper.createElement("MultiplicativeExpr")
                    .addAttribute("operator", token);
            m.add(uLeft);
            m.add(uRight);
            uLeft = m;
            token = tokenStream.nextToken();
        }

        return uLeft;
    }

    private static Element unaryExpr(TokenStream tokenStream)
    {

        if (tokenStream.nextToken().equals("("))
        {
            tokenStream.getToken();
            Element u = assignmentExpr(tokenStream);
            tokenStream.getToken();
            return u;
        }

        String token = tokenStream.nextToken();
        if (token.equals("+") || token.equals("-"))
        {
            tokenStream.getToken();
            Element u = DocumentHelper.createElement("UnaryExpr")
                    .addAttribute("operator", token);
            u.add(assignmentExpr(tokenStream));
            return u;
        }

        if (token.equals("~"))
        {
            tokenStream.getToken();
            Element u = DocumentHelper.createElement("LogicalNotExpr")
                    .addAttribute("operator", token);
            u.add(assignmentExpr(tokenStream));
            return u;
        }

        //多返回值函数调用的返回值列表
        if (token.equals("["))
        {
            Element returnList = DocumentHelper.createElement("ReturnList")
                    .addAttribute("operator", "[]");
            tokenStream.getToken(); //[
            do
            {
                returnList.add(assignmentExpr(tokenStream));
                token = tokenStream.getToken();
            }
            while (token.equals(","));

            return returnList;
        }


        //读到标识符,查看标识符后一个token
        TokenStream newStream = new TokenStream(tokenStream.getLine());
        newStream.getToken();
        String nextToken = newStream.nextToken();
        if (nextToken != null)
        {
            if (nextToken.equals("("))
            {
                if (isFunctionName(token))  //函数调用
                {
                    Element name = DocumentHelper.createElement("FunctionName")
                            .addText(tokenStream.getToken());
                    Element functionCall = DocumentHelper.createElement("FunctionCall")
                            .addAttribute("operator", "()");
                    functionCall.add(name);
                    Element ArgumentList = DocumentHelper.createElement("ArgumentList");
                    tokenStream.getToken(); //(
                    do
                    {
                        ArgumentList.add(assignmentExpr(tokenStream));
                        token = tokenStream.getToken();
                    }
                    while (token.equals(","));

                    functionCall.add(ArgumentList);
                    return functionCall;
                }
                else    //取地址,对矢量而言
                {
                    token = tokenStream.getToken();
                    Element id = DocumentHelper.createElement("Matrix")
                            .addAttribute("type", VarTypeHelper.getPrefix(token))
                            .addAttribute("mtype", VarTypeHelper.getPostfix(token))
                            .addText(token);
                    Element i = DocumentHelper.createElement("IndexExpr")
                            .addAttribute("operator", "()");
                    tokenStream.getToken();
                    Element r = rangeExpr(tokenStream);
                    tokenStream.getToken();
                    i.add(id);
                    i.add(r);
                    return i;
                }
            }

            if (nextToken.equals("."))  //结构体取成员
            {
                token = tokenStream.getToken();
                Element id = DocumentHelper.createElement("Identifier")
                        .addAttribute("type", "struct *")
                        .addText(token);
                Element a = DocumentHelper.createElement("AccessField")
                        .addAttribute("operator", ".");
                a.add(id);
                tokenStream.getToken();
                token = tokenStream.getToken();
                String postfix = VarTypeHelper.getPostfix(token);
                if (postfix != null)
                {
                    Element m = DocumentHelper.createElement("Matrix")
                            .addAttribute("type", VarTypeHelper.getPrefix(token))
                            .addAttribute("mtype", postfix)
                            .addText(token);
                    a.add(m);
                }
                else
                {
                    Element f = DocumentHelper.createElement("Identifier")
                            .addAttribute("type", VarTypeHelper.getPrefix(token))
                            .addText(token);
                    a.add(f);
                }
                return a;
            }
        }
        //遇到语句尾，返回该token对应的Element
        return getElementOfToken(tokenStream);
    }

    private static Element getElementOfToken(TokenStream tokenStream)
    {
        String token = tokenStream.getToken();
        String postfix = VarTypeHelper.getPostfix(token);
        if (postfix != null)     //是矢量
        {
            //统一使用Matrix，这里不区分
            return DocumentHelper.createElement("Matrix")
                    .addAttribute("type", VarTypeHelper.getPrefix(token))
                    .addAttribute("mtype", postfix)
                    .addText(token);
        }

        else if (Character.isDigit(token.charAt(0)))
        {
            return DocumentHelper.createElement("Constant")
                    .addText(token);
        }
        else
        {
            return DocumentHelper.createElement("Identifier")
                    .addAttribute("type", VarTypeHelper.getPrefix(token))
                    .addText(token);
        }
    }

    private static Element rangeExpr(TokenStream tokenStream)
    {
        Element r = DocumentHelper.createElement("RangeExpr");
        r.add(colonExpr(tokenStream));
        //todo 暂未考虑单独使用数字下标的情况，即未考虑A(1,1)这种情况
        while (tokenStream.nextToken().equals(","))
        {
            tokenStream.getToken();
            r.add(colonExpr(tokenStream));
        }
        return r;
    }

    private static Element colonExpr(TokenStream tokenStream)
    {
        Element c = DocumentHelper.createElement("ColonExpr")
                .addAttribute("operator", ":");
        String token = tokenStream.nextToken();
        if (token.equals(":"))
        {
            tokenStream.getToken();
        }
        else
        {
            c.add(logicalOrExpr(tokenStream));
            while (tokenStream.nextToken().equals(":"))
            {
                tokenStream.getToken();
                c.add(logicalOrExpr(tokenStream));
                if (tokenStream.nextToken() == null)
                {
                    break;
                }
            }
        }
        return c;
    }

    public static void main(String[] args)
    {
        Scanner in = new Scanner(System.in);
        String input = in.nextLine();
        Element element = ExprProcessor.getExprElement(input);

        //输出XML文件
        try
        {
            OutputFormat format = OutputFormat.createPrettyPrint();
            format.setExpandEmptyElements(true);        //避免自闭合标签，前后统一
            XMLWriter writer = new XMLWriter(new FileOutputStream("输出测试.xml"), format);
            writer.write(element);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }
}
