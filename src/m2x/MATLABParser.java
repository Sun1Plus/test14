package m2x;

import m2x.utils.TokenStream;
import m2x.utils.OperationRecognizerDemo;
import static m2x.utils.ExprProcessor.getExprElement;
import static m2x.utils.VarTypeHelper.*;
import org.dom4j.*;
import org.dom4j.io.*;
import org.jetbrains.annotations.*;
import java.io.*;
import java.util.*;

/*
 * 处理MATLAB数字化样机，输出相应的XML文件
 * */
@SuppressWarnings("ConstantConditions")
public class MATLABParser
{
    private Scanner scanner = null; //用于在parseBlock中读取matlab文件

    //对代码块进行递归向下解析
    //返回值是为了处理if语句中出现elseif和else的情况
    @Nullable
    private String parseBlock(Element currentElement)
    {
        while (scanner.hasNextLine())
        {
            //读入的line是没有前后空格且没有结尾分号的matlab语句
            String line = scanner.nextLine().trim().replace(";", "");

            //过滤空行和行注释
            if (line.equals("") || (line.startsWith("%") && !line.startsWith("%{") && !line.startsWith("%#")))      //空行、单行注释(或分段等)
            {
                continue;
            }

            //过滤整段段注释
            if (line.startsWith("%{"))
            {
                while (!line.endsWith("%}"))
                {
                    line = scanner.nextLine().trim();
                }
                continue;
            }

            //处理语句末尾注释
            //偷懒直接查找语句中是否存在%
            int index = line.indexOf('%');
            if (index != -1 && !line.startsWith("%#"))
            {
                if (index != line.length() - 1
                        && line.charAt(index + 1) == '{')   //段注释
                {
                    String commentLine = scanner.nextLine().trim();
                    while (!commentLine.endsWith("%}"))
                    {
                        commentLine = scanner.nextLine().trim();
                    }
                }
                line = line.substring(0, index);    //去掉%之后的内容
            }

            if (line.startsWith("%#"))   //当前行是标记
            {
                //需要进一步分情况讨论
                TokenStream tokenStream = new TokenStream(line);
                tokenStream.getToken(); //%#
                //TODO 是否可以取消语言标记？暂时不要
                /*
                String token = tokenStream.getToken();
                if (token.equals("matrix"))
                {
                    parseMatrixMark(tokenStream);
                }
                else    //并行标记
                {
                    currentElement.addElement("mark").setText(tokenStream.getOldLine());
                }
                */
                currentElement.addElement("mark").setText(tokenStream.getLine());
            }
            else        //MATLAB语句
            {
                while (line.endsWith("..."))        //续行符
                {
                    line = line.replace("...", " ") + scanner.nextLine().trim();
                }

                TokenStream tokenStream = new TokenStream(line);
                String token = tokenStream.getToken();

                //noinspection IfCanBeSwitch
                if (token.equals("function"))           //函数的情况
                {
                    parseFunction(tokenStream, currentElement);
                }
                else if (token.equals("if"))            //if语句块
                {
                    parseIfStatement(tokenStream, currentElement);
                }
                else if (token.equals("elseif") || token.equals("else"))    //elseif和else语句块
                {
                    return line;    //return 到if stmt的处理程序中，处理elseif或else的情况
                                    //这是因为没有end帮助判断结束，不能统一处理，必须单独对待
                }
                else if (token.equals("for"))           //for语句块
                {
                    parseForStatement(tokenStream, currentElement);
                }
                else if (token.equals("end"))           //块结束
                {
                    return null;
                }
                else if (token.equals("return"))        //todo return返回值的情况（目前没有）
                {
                    currentElement.addElement("return");
                }
                else if (token.equals("continue"))
                {
                    currentElement.addElement("continue");
                }
                else        //表达式或算子
                {
                    OperationRecognizerDemo or = OperationRecognizerDemo.getInstance();
                    if (or.isOperation(line))  //算子就只放原语句，后续再处理
                    {
                        currentElement.addElement("Operation").setText(line);
                    }
                    else    //普通表达式
                    {
                        currentElement.add(getExprElement(line));
                    }
                }
            }
        }

        return null;
    }

    //用于处理函数体
    private void parseFunction(TokenStream tokenStream, Element currentElement)
    {

        Element Function = currentElement.addElement("Function");

        //处理返回值
        Element ReturnList = Function.addElement("ReturnList");
        String token = tokenStream.getToken();
        if (token.equals("["))
        {
            while (!(token = tokenStream.getToken()).equals("]"))
            {
                if (!token.equals(","))
                {
                    addToList(token, ReturnList);
                }
            }
            tokenStream.getToken();     // = 符号
        }
        else    //单个输出
        {
            if (tokenStream.nextToken().equals("="))        //单输出
            {
                addToList(token, ReturnList);
                tokenStream.getToken();     // = 符号
            }
        }

        //处理函数名
        Function.addElement("FunctionName").addText(tokenStream.getToken());

        //处理参数
        Element ArgumentList = Function.addElement("ArgumentList");
        tokenStream.getToken();     // ( 符号
        if (!tokenStream.nextToken().equals(")"))    //有参数
        {
            while (!(token = tokenStream.getToken()).equals(")"))
            {
                if (!token.equals(","))
                {
                    addToList(token, ArgumentList);
                }
            }
        }

        //处理函数体内部语句
        parseBlock(Function);
    }

    //parseFunction()的辅助函数
    private void addToList(String token, Element list)
    {
        if (isMatrix(token))
        {
            list.addElement("Matrix")
                    .addAttribute("type", getPrefix(token))
                    .addAttribute("mtype", getPostfix(token))
                    .addText(token);
        }
        else
        {
            list.addElement("Identifier")
                    .addAttribute("type", getPrefix(token))
                    .addText(token);
        }
    }

    //用于处理if语句块
    private void parseIfStatement(TokenStream tokenStream, Element currentElement)
    {
        Element IfStatement = currentElement.addElement("IfStatement");
        Element IfBlock = IfStatement.addElement("IfBlock");
        IfBlock.add(getExprElement(tokenStream.getLine()));
        String line = parseBlock(IfBlock);
        while (line != null)
        {
            TokenStream ts = new TokenStream(line);
            if (ts.getToken().equals("elseif"))
            {
                Element ElseifBlock = IfStatement.addElement("ElseifBlock");
                ElseifBlock.add(getExprElement(ts.getLine()));
                line = parseBlock(ElseifBlock);
            }
            else
            {
                Element ElseBlock = IfStatement.addElement("ElseBlock");
                parseBlock(ElseBlock);
                break;  //只有一个对应的else
            }
        }
    }

    //用于处理for语句块
    private void parseForStatement(TokenStream tokenStream, Element currentElement)
    {
        Element ForStatement = currentElement.addElement("ForStatement");
        ForStatement.add(getExprElement(tokenStream.getLine()));
        parseBlock(ForStatement);
    }

    //入口函数，输出XML文件到output文件夹中
    public void getXML(String inputFile, String outputFile)
    {
        //后续算子识别
        //https://www.cnblogs.com/xiatian3452/p/10952789.html Dom4J生成XML
        //创建Document
        Document doc = DocumentHelper.createDocument();
        Element currentElement = doc.addElement("Program");

        //解析MATLAB代码
        File file = new File(inputFile);

        try
        {
            scanner = new Scanner(file);
        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();
        }

        parseBlock(currentElement);

        //输出XML文件
        try
        {
            OutputFormat format = OutputFormat.createPrettyPrint();
            format.setExpandEmptyElements(true);        //避免自闭合标签，前后统一
            XMLWriter writer = new XMLWriter(new FileOutputStream(outputFile), format);
            writer.write(doc);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

    }

    public static void main(String[] args)
    {
        //创建输出路径
        new File("./output").mkdir();
        try
        {
            new File("./output/out.xml").createNewFile();
        } catch (IOException e)
        {
            e.printStackTrace();
        }
        new MATLABParser().getXML("./input/m.txt", "./output/out.xml");
    }
}
