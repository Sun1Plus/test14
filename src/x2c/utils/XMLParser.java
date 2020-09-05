package x2c.utils;
import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.omg.PortableInterceptor.SYSTEM_EXCEPTION;

import javax.naming.ldap.PagedResultsControl;
import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import x2c.utils.Pair;
import x2c.utils.OperationParser;

public class XMLParser {
    private ArrayList<String> Cfile=new ArrayList<String>();
    private String resultProgram="";
    private ArrayList<Pair<String,String>> identifierArray=new ArrayList<>();
    private ArrayList<String> nameSet=new ArrayList<String>() {
        @Override
        public boolean equals(Object o) {
            if (o == null) {
                return false;
            }
            if (o instanceof String) {
                Iterator lit=nameSet.iterator();
                while(lit.hasNext())
                {
                    if(lit.next().equals(o))
                        return true;
                }
            }
            return false;
        }
    };

    private String ParseIfStatement(Element curElement)
    {
        String resultValue="";
        Iterator itIfStatement=curElement.elementIterator();
        while(itIfStatement.hasNext())
        {
            Element ifBlockElement=(Element) itIfStatement.next();
            if(ifBlockElement.getName().equals("IfBlock"))
                resultValue+=ParseBlock(ifBlockElement);
            else if(ifBlockElement.getName().equals("ElseifBlock"))
                resultValue+=ParseBlock(ifBlockElement);
            else if(ifBlockElement.getName().equals("ElseBlock"))
                resultValue+=ParseBlock(ifBlockElement);
            else
                System.out.println("ERROR: IfStatement's son node Error!!!!!!!!!!!!!!!!!");
        }
        return resultValue;
    }

    private String ParseIfBlcok(Element curElement)
    {
        String resultValue="";
        // if(
        //Cfile.add("if( ");
        resultValue+="if( ";
        Iterator itIfBlock=curElement.elementIterator();

        //判断条件部分
        if(itIfBlock.hasNext())
        {
            Element equalityExprElement=(Element) itIfBlock.next();
            resultValue+= ParseBlock(equalityExprElement);
        }


        // ){
        //Cfile.add(")\n{");
        resultValue+="){\n";
        //if里面的语句
        while(itIfBlock.hasNext())
        {
            Element ifBlockElement=(Element) itIfBlock.next();
            resultValue+= ParseBlock(ifBlockElement);
        }

        // }
        //Cfile.add("}\n");
        resultValue+="}\n";
        return  resultValue;
    }

    private String ParseElseifBlcok(Element curElement)
    {
        String resultValue="";
        // else if(
        //Cfile.add("else if( ");
        resultValue+="else if( ";
        Iterator itElseifBlock=curElement.elementIterator();

        //判断条件部分
        if(itElseifBlock.hasNext())
        {
            Element equalityExprElement=(Element) itElseifBlock.next();
            resultValue+= ParseBlock(equalityExprElement);
        }
        // ){
        //Cfile.add("){\n");
        resultValue+="){\n";

        //else if里面的语句
        while(itElseifBlock.hasNext())
        {
            Element elseifBlockElement=(Element) itElseifBlock.next();
            resultValue+= ParseBlock(elseifBlockElement);
        }

        // }
        //Cfile.add("}\n");
        resultValue+="}\n";
        return resultValue;
    }

    private String ParseElseBlcok(Element curElement)
    {
        String resultValue="";
        // else{
        //Cfile.add("else{\n");
        resultValue+="else{\n";
        Iterator itElseBlock=curElement.elementIterator();

        //else if里面的语句
        while(itElseBlock.hasNext())
        {
            Element elseBlockElement=(Element) itElseBlock.next();
            resultValue+=ParseBlock(elseBlockElement);
        }

        // }
        //Cfile.add("}\n");
        resultValue+="}\n";
        return resultValue;
    }

    private String ParseForStatement(Element curElement)
    {
        String resultValue="";
        // for(
        //Cfile.add("for( ");
        resultValue+="for( ";

        Iterator itForStatement=curElement.elementIterator();

        //条件语句部分
        if (itForStatement.hasNext())
        {
            Element assignmentExprElement=(Element) itForStatement.next();
            // 当前 assignmentExprElement = <AssignmentExpr Operator="=">
            if(assignmentExprElement.getName().equals("AssignmentExpr"))
            {
                // 提取 assignmentExpr的符号（Operator Attribute）
                String operatorAssignmentExpr = "";

                // 提取 assignmentExpr的主变量identifier
                String identifierAssignmentExpr="";

                // 提取 assignmentExpr的循环初始值和终止值
                String startAssignmentExpr="";
                String endAssignmentExpr="";

                List<Attribute> assignmentExprAttributes = assignmentExprElement.attributes();
                for (Attribute attribute1 : assignmentExprAttributes)
                    //operatorEx : "="
                    operatorAssignmentExpr = attribute1.getValue();


                Iterator itAssignmentExpr = assignmentExprElement.elementIterator();
                while (itAssignmentExpr.hasNext())
                {
                    // chooseElement 目的是分出循环条件的主变量、初始值、终止值
                    Element chooseElement=(Element) itAssignmentExpr.next();
                    // TODO: 考虑这里的类型问题，以及类型怎么输出，以及是否需要定义新变量
                    if(chooseElement.getName().equals("Identifier"))
                    {
                        // 得到主变量（identifierAssignmentExpr）
                        identifierAssignmentExpr=ParseIdentifier(chooseElement);
                    }
                    else if(chooseElement.getName().equals("ColonExpr"))
                    {
                        // 得到初始值(startAssignmentExpr)和终止值(endAssignmentExpr)
                        Pair<String, String> tempPair=ParseColonExpr(chooseElement);
                        startAssignmentExpr=tempPair.v1;
                        endAssignmentExpr=tempPair.v2;
                    }
                    else
                    {
                        // TODO：循环体里不应该有其他的东西了，如果有，是特殊情况，要报错
                        System.out.println("ERROR: 循环体部分出错，含有其他情况");
                    }
                }
//                Cfile.add(identifierAssignmentExpr);
                resultValue+=identifierAssignmentExpr;
//                Cfile.add(operatorAssignmentExpr);
                resultValue+=operatorAssignmentExpr;
//                Cfile.add(startAssignmentExpr);
                resultValue+=startAssignmentExpr;

                // ;
//                Cfile.add(";");
                resultValue+=";";
//                Cfile.add(identifierAssignmentExpr);
                resultValue+=identifierAssignmentExpr;
//                Cfile.add("<=");
                resultValue+="<=";
//                Cfile.add(endAssignmentExpr);
                resultValue+=endAssignmentExpr;

                // ;
//                Cfile.add(";");
                resultValue+=";";
//                // identify = identify + 1
//                Cfile.add(identifierAssignmentExpr);
                resultValue+=identifierAssignmentExpr;
//                Cfile.add("++");
                resultValue+="++";
            }
        }

        // ){
        //Cfile.add("){\n");
        resultValue+="){\n";

        //for循环内部语句
        while(itForStatement.hasNext())
        {
            Element forStatementElement=(Element) itForStatement.next();
            resultValue+= ParseBlock(forStatementElement);
        }

        // }
        //Cfile.add("}\n");
        resultValue+="}\n";
        return resultValue;
    }

    private String ParseIdentifier(Element curElement)
    {
        String typeIdentifier="";
        List<Attribute> attributes = curElement.attributes();
        for (Attribute attribute1 : attributes)
            //operatorEx : "="
            typeIdentifier = attribute1.getValue();

        String valueIdentifier=curElement.getStringValue();

        //TODO: 要做定义和声明的检测，或者加一个set,设计一个包含作用域的结构,匹配问题也要考虑
        //TODO: equals方法以后要改，不能这么起名字
        if(!nameSet.equals(valueIdentifier))
        {
            Pair<String, String> tempPair = new Pair<>(valueIdentifier, typeIdentifier);
            identifierArray.add(tempPair);
            nameSet.add(valueIdentifier);
        }
        return valueIdentifier;
    }

    //TODO: 这里之后讨论一下，这个标签需要特殊对待一下
    private Pair<String, String> ParseColonExpr(Element curElement)
    {
        Pair<String, String> resultValue=new Pair<>("","");
        Iterator itColonExpr=curElement.elementIterator();
        if(itColonExpr.hasNext())
        {
            Element v1ColonExpr=(Element) itColonExpr.next();
            resultValue.v1=ParseBlock(v1ColonExpr);
        }
        if(itColonExpr.hasNext())
        {
            System.out.println("Notice: <ColonExpr>只有一个标签。");
            Element v2ColonExpr=(Element) itColonExpr.next();
            resultValue.v2=ParseBlock(v2ColonExpr);
        }
        if(itColonExpr.hasNext())
        {
            //Error:这里就应该只有两个值
            System.out.println("ERROR: for循环pair<>这里应该只有两个值.");
        }
        return resultValue;
    }

    private String ParseAssignmentExpr(Element curElement)
    {
        String resultValue="";
        String operatorAssignmentExpr="";
        String typeLeftPartofAssignmentExpr="";
        String leftPartOfAssignmentExpr="";
        String rightPartOfAssignmentExpr="";
        List<Attribute> curAssignmentExprAttributes = curElement.attributes();
        for (Attribute attribute1 : curAssignmentExprAttributes)
            //operatorEx : "="
            operatorAssignmentExpr = attribute1.getValue();

        Iterator itAssignmentExpr=curElement.elementIterator();
        // AssignmentExpr的左边
        if(itAssignmentExpr.hasNext())
        {
            Element leftPartOfAssignmentExprElement = (Element) itAssignmentExpr.next();
            // 如果变量在AssignmentExpr的左边，要考虑先声明
            if(leftPartOfAssignmentExprElement.getName().equals("Identifier") && (!nameSet.equals(leftPartOfAssignmentExprElement.getStringValue())))
            {
                typeLeftPartofAssignmentExpr=leftPartOfAssignmentExprElement.attributeValue("type");
            }
            else if(leftPartOfAssignmentExprElement.getName().equals("Matrix") && (!nameSet.equals(leftPartOfAssignmentExprElement.getStringValue())))
            {
                //TODO: Matrix暂时先放一个Matrix作为类型
                //typeLeftPartofAssignmentExpr=leftPartOfAssignmentExprElement.attributeValue("type");
                typeLeftPartofAssignmentExpr="Matrix ";
                //TODO: Matrix要考虑一下[ , ]里面的处理
            }

            leftPartOfAssignmentExpr = ParseBlock(leftPartOfAssignmentExprElement);
        }

        if(itAssignmentExpr.hasNext())
        {
            Element rightPartOfAssignmentExprElement = (Element) itAssignmentExpr.next();
            rightPartOfAssignmentExpr = ParseBlock(rightPartOfAssignmentExprElement);
            // 如果在AssignmentExpr的右边，要检查是否已经被声明

            //TODO：检查变量是否声明
        }

//        Cfile.add(leftPartOfAssignmentExpr+operatorAssignmentExpr+rightPartOfAssignmentExpr);
        resultValue = resultValue + typeLeftPartofAssignmentExpr + " " + leftPartOfAssignmentExpr + operatorAssignmentExpr + rightPartOfAssignmentExpr+";\n";
        return resultValue;
    }

    //TODO: 目前这个函数可以应用于所有的“ A op B ”形式调用
    private String ParsemultiplicativeExpr(Element curElement)
    {
        String resultValue="";
        String operatorMultiplicativeExpr="";
        String leftMultiplicativeExpr="";
        String rightMultiplicativeExpr="";

        List<Attribute> curMultiplicativeAttributes = curElement.attributes();
        for (Attribute attribute1 : curMultiplicativeAttributes)
            //operatorEx : "="
            operatorMultiplicativeExpr=attribute1.getValue();

        Iterator itMultiplicativeExpr=curElement.elementIterator();
        if(itMultiplicativeExpr.hasNext())
        {
            Element leftExprElement=(Element) itMultiplicativeExpr.next();
            leftMultiplicativeExpr= ParseBlock(leftExprElement);
        }
        if(itMultiplicativeExpr.hasNext())
        {
            Element rightExprElement=(Element) itMultiplicativeExpr.next();
            rightMultiplicativeExpr=ParseBlock(rightExprElement);
        }
        if(itMultiplicativeExpr.hasNext())
        {
            //Error:这里就应该只有两个值
            System.out.println("ERROR: Multiplicative表达式这里应该只有两个值.");
        }
        resultValue="("+leftMultiplicativeExpr+operatorMultiplicativeExpr+rightMultiplicativeExpr+")";

        return resultValue;
    }

    private String ParseConstant(Element curElement)
    {
        String resultValue=curElement.getStringValue();

        return resultValue;
    }

    private String ParseequalityExpr(Element curElement)
    {
        String resultValue="";
        String operatorequalityExpr="";
        String leftequalityExpr="";
        String rightequalityExpr="";

        List<Attribute> curequalityAttributes = curElement.attributes();
        for (Attribute attribute1 : curequalityAttributes)
            //operatorEx : "="
            operatorequalityExpr=attribute1.getValue();

        Iterator itequalityExpr=curElement.elementIterator();
        if(itequalityExpr.hasNext())
        {
            Element leftExprElement=(Element) itequalityExpr.next();
            leftequalityExpr= ParseBlock(leftExprElement);
        }
        if(itequalityExpr.hasNext())
        {
            Element rightExprElement=(Element) itequalityExpr.next();
            rightequalityExpr=ParseBlock(rightExprElement);
        }
        if(itequalityExpr.hasNext())
        {
            //Error:这里就应该只有两个值
            System.out.println("ERROR: equality表达式这里应该只有两个值.");
        }
        resultValue=leftequalityExpr+operatorequalityExpr+rightequalityExpr;

        return resultValue;
    }

    private String ParseFunctionName(Element curElement)
    {
        String resultValue = curElement.getStringValue();

        return resultValue;
    }

    private String Parsereturn(Element curElement)
    {
        String resultValue=curElement.getStringValue();

        return resultValue;
    }

    private String ParseRelationalExpr(Element curElement)
    {
        String resultValue="";
        String operatorRelationalExpr="";
        List<Attribute> curRelationalExprAttributes = curElement.attributes();
        for (Attribute attribute1 : curRelationalExprAttributes)
            operatorRelationalExpr=attribute1.getValue();
        if(operatorRelationalExpr.equals("&gt;"))
            operatorRelationalExpr=">";
        else if(operatorRelationalExpr.equals("&lt;"))
            operatorRelationalExpr="<";

        Iterator itRelationalExpr=curElement.elementIterator();
        if(itRelationalExpr.hasNext())
        {
            Element leftLogicalAndExprElement=(Element) itRelationalExpr.next();
            resultValue += ParseBlock(leftLogicalAndExprElement);
        }
        else
        {
            System.out.println("Error: <RelationalExpr>没有第一个标签！");
        }

        resultValue += operatorRelationalExpr;

        if(itRelationalExpr.hasNext())
        {
            Element rightLogicalAndExprElement=(Element) itRelationalExpr.next();
            resultValue += ParseBlock(rightLogicalAndExprElement);
        }
        else
        {
            System.out.println("Error: <RelationalExpr>没有第二个标签！");
        }


        return resultValue;
    }

    private String ParseLogicalAndExpr(Element curElement)
    {
        String resultValue="( ";

        Iterator itLogicalAndExpr=curElement.elementIterator();
        if(itLogicalAndExpr.hasNext())
        {
            Element leftLogicalAndExprElement=(Element) itLogicalAndExpr.next();
            resultValue+=ParseBlock(leftLogicalAndExprElement);
        }
        else
        {
            System.out.println("Error: <LogicalAndExpr>没有第一个标签！");
        }

        resultValue+=" && ";

        if(itLogicalAndExpr.hasNext())
        {
            Element rightLogicalAndExprElement=(Element) itLogicalAndExpr.next();
            resultValue += ParseBlock(rightLogicalAndExprElement);
        }
        else
        {
            System.out.println("Error: <LogicalAndExpr>没有第二个标签！");
        }

        resultValue+=")";

        return resultValue;
    }

    //TODO: 目前先只考虑一个返回值的情况，要保证第一个标签是返回值及其类型
    private String ParseFunction(Element curElement)
    {
        String resultValue="";
        Iterator itFunction = curElement.elementIterator();

        //处理函数的返回值和类型
        if(itFunction.hasNext())
        {
            Element returnListElement=(Element) itFunction.next();
            if(returnListElement.getName().equals("ReturnList"))
                resultValue+=ParseBlock(returnListElement);
            else
                System.out.println("Error: <Function>的第一个子标签不是<ReturnList>！");
        }

        //处理函数名
        if(itFunction.hasNext())
        {
            Element functionNameElement=(Element) itFunction.next();
            if(functionNameElement.getName().equals("FunctionName"))
                resultValue+=ParseBlock(functionNameElement);
            else
                System.out.println("Error: <Function>的第二个子标签不是<FunctionName>!");
        }

        //（
        resultValue+="(";

        //处理参数列表
        if(itFunction.hasNext())
        {
            Element argumentListElement=(Element) itFunction.next();
            if(argumentListElement.getName().equals("ArgumentList"))
                resultValue+=ParseBlock(argumentListElement);
            else
                System.out.println("Error: <Function>的第三个子标签不是<ArgumentList>!");
        }

        //）{
        resultValue+="){\n";

        //处理函数体
        while(itFunction.hasNext())
        {
            Element functionBodyElement=(Element) itFunction.next();
            resultValue+=ParseBlock(functionBodyElement);
        }

        //}
        resultValue+="}\n";
        return resultValue;
    }

    private String ParseFunctionCall(Element curElement)
    {
        String resultValue="";
        // functionName()
        Iterator itFunctionCall=curElement.elementIterator();
        if(itFunctionCall.hasNext())
        {
            Element fuctionNameElement=(Element) itFunctionCall.next();
            if(fuctionNameElement.getName().equals("FunctionName"))
                resultValue+=ParseBlock(fuctionNameElement);
            else
                System.out.println("Error: <FunctionCall>的第一个子标签不是<FunctionName>!");
        }

        //(
        resultValue+="(";

        if(itFunctionCall.hasNext())
        {
            Element argumentListElement=(Element) itFunctionCall.next();
            if(argumentListElement.getName().equals("ArgumentList"))
                resultValue+=ParseBlock(argumentListElement);
            else
            {
                System.out.println("Error: <FunctionCall>的第二个子标签不是<ArgumentList>!");
            }
        }

        resultValue+=")";

        return resultValue;
    }

    private String ParseArgumentList(Element curElement)
    {
        String resultValue="";
        Iterator itArgumentList=curElement.elementIterator();
        while(itArgumentList.hasNext())
        {
            Element argumentElement=(Element) itArgumentList.next();
            resultValue+=argumentElement.attributeValue("type")+" ";
            resultValue+=ParseBlock(argumentElement)+", ";
        }
        resultValue=resultValue.substring(0,resultValue.length()-2);

        return resultValue;
    }

    //TODO:ArgumentList是为了声明用的，带类型；ParaList是函数调用时用的，不带参数类型
    private String ParseParaList(Element curElement)
    {
        String resultValue="";
        Iterator itArgumentList=curElement.elementIterator();
        while(itArgumentList.hasNext())
        {
            Element argumentElement=(Element) itArgumentList.next();
            //resultValue+=argumentElement.attributeValue("type")+" ";
            resultValue+=ParseBlock(argumentElement)+", ";
        }
        resultValue=resultValue.substring(0,resultValue.length()-2);

        return resultValue;
    }

    //TODO： 这里暂时不通过ParseBlock解决，直接调用ParseXXX函数
    private String ParseReturnList(Element curElement)
    {
        String resultValue="";
        Iterator itReturnList=curElement.elementIterator();
        if(itReturnList.hasNext())
        {
            Element identifierElement=(Element) itReturnList.next();
            if(identifierElement.getName().equals("Identifier"))
            {
                List<Attribute> identifierAttributes = identifierElement.attributes();
                for (Attribute attribute1 : identifierAttributes)
                    //operatorEx : "="
                    resultValue+=attribute1.getValue();
                resultValue+=" ";
                resultValue+= ParseBlock(identifierElement);
            }
            else if(identifierElement.getName().equals("Matrix"))
            {
                List<Attribute> matrixAttributes = identifierElement.attributes();
                for (Attribute attribute1 : matrixAttributes)
                {   //operatorEx : "="
                    resultValue += attribute1.getValue();
                    resultValue += " ";
                }
                resultValue+=ParseBlock(identifierElement);
            }
            else
            {
                System.out.println("Error：<ReturnList>返回的既不是<Identifier>也不是<Matrix>！");
            }
        }

        return resultValue;
    }

    private String ParseIndexExpr(Element curElement)
    {
        String resultValue="";
        Iterator itIndexExpr=curElement.elementIterator();
        if(itIndexExpr.hasNext())
        {
            Element matrixElement=(Element) itIndexExpr.next();
            if(matrixElement.getName().equals("Matrix") && (!nameSet.equals(matrixElement.getStringValue())))
            {
                //TODO: Matrix暂时先放一个Matrix作为类型
                //typeLeftPartofAssignmentExpr=leftPartOfAssignmentExprElement.attributeValue("type");
                resultValue+="Matrix ";
                //TODO: Matrix要考虑一下[ , ]里面的处理
            }
            resultValue+=ParseBlock(matrixElement);
        }

        resultValue+="[";

        if(itIndexExpr.hasNext())
        {
            Element matrixElement=(Element) itIndexExpr.next();
            resultValue+=ParseBlock(matrixElement);
        }

        resultValue+="]";

        return resultValue;
    }

    private String ParserangeExpr(Element curElement)
    {
        String resultValue="";
        Pair<String, String> tempValue=new Pair<>("","");
        Iterator itRangeExpr=curElement.elementIterator();
        if(itRangeExpr.hasNext())
        {
            Element colonExprElement=(Element) itRangeExpr.next();
            tempValue=ParseColonExpr(colonExprElement);
            resultValue+=tempValue.v1;
            if(!tempValue.v2.isEmpty())
            {
                resultValue+=" : ";
                resultValue+=tempValue.v2;
            }
        }

        return resultValue;
    }

    //TODO：这里先留一个接口
    private String ParseOperation(Element curElement)
    {
        OperationParser opParser=new OperationParser();
        String resultValue="";
        resultValue+=opParser.transform(curElement);

        return resultValue;
    }


    //解析Block
    private String ParseSetBlock(Element curElement)
    {
        String resultValue="";
        Iterator itSetBlock=curElement.elementIterator();

        //else if里面的语句
        while(itSetBlock.hasNext())
        {
            Element elseBlockElement=(Element) itSetBlock.next();
            resultValue+=ParseBlock(elseBlockElement);
        }

        return resultValue;
    }

    //解析View
    private String ParseSetView(Element curElement)
    {
        String resultValue="";
        Iterator itSetView=curElement.elementIterator();

        //else if里面的语句
        while(itSetView.hasNext())
        {
            Element elseBlockElement=(Element) itSetView.next();
            resultValue+=ParseBlock(elseBlockElement);
        }

        return resultValue;
    }

    private String ParseBlock(Element curElement)
    {
        String result="";
        String curName=curElement.getName();
        if(curName.equals("IfStatement"))
        {
            result+=ParseIfStatement(curElement);
//            ParseIfStatement(curElement);
        }
        else if(curName.equals("IfBlock"))
        {
            result+=ParseIfBlcok(curElement);
//            ParseIfBlcok(curElement);
        }
        else if(curName.equals("ElseifBlock"))
        {
            result+=ParseElseifBlcok(curElement);
//            ParseElseifBlcok(curElement);
        }
        else if(curName.equals("ElseBlock"))
        {
            result+=ParseElseBlcok(curElement);
//            ParseElseBlcok(curElement);
        }
        else if(curName.equals("ForStatement"))
        {
            result+=ParseForStatement(curElement);
//            ParseForStatement(curElement);
        }
        else if(curName.equals("AssignmentExpr"))
        {
            result+=ParseAssignmentExpr(curElement);
        }
        else if (curName.equals("MultiplicativeExpr")) {
            result+=ParsemultiplicativeExpr(curElement);
        }
        else if (curName.equals("ParseIdentifier")) {
            result+=ParseIdentifier(curElement);
        }
        else if(curName.equals("Constant"))
        {
            result+=ParseConstant(curElement);
        }
        else if(curName.equals("EqualityExpr"))
        {
            result+=ParseequalityExpr(curElement);
        }
        else if(curName.equals("Identifier"))
        {
            result+=ParseIdentifier(curElement);
        }
        else if(curName.equals("Matrix"))
        {
            result+=ParseIdentifier(curElement);
        }
        else if(curName.equals("AdditiveExpr"))
        {
            result+=ParsemultiplicativeExpr(curElement);
        }
        else if (curName.equals("Function"))
        {
            result+=ParseFunction(curElement);
        }
        else if(curName.equals("FunctionName"))
        {
            result=ParseFunctionName(curElement);
        }
        else if(curName.equals("ArgumentList"))
        {
            result=ParseArgumentList(curElement);
        }
        else if(curName.equals("ReturnList"))
        {
            result=ParseReturnList(curElement);
        }
        else if(curName.equals("Return"))
        {
            result+="return"+Parsereturn(curElement)+";";
        }
        else if(curName.equals("AccessField"))
        {
            result+=ParsemultiplicativeExpr(curElement);
        }
        else if(curName.equals("FunctionCall"))
        {
            result+=ParseFunctionCall(curElement);
        }
        else if(curName.equals("IndexExpr"))
        {
            result+=ParseIndexExpr(curElement);
        }
        else if(curName.equals("RangeExpr"))
        {
            result+=ParserangeExpr(curElement);
        }
        else if(curName.equals("RelationalExpr"))
        {
            result+=ParseRelationalExpr(curElement);
        }
        else if(curName.equals("continue"))
        {
            result+="continue;\n";
        }
        else if(curName.equals("LogicalAndExpr"))
        {
            result+=ParseLogicalAndExpr(curElement);
        }
        else if(curName.equals("Operation"))
        {
            result+=ParseOperation(curElement);
        }
        else if(curName.equals("SetBlock"))
        {
            result+=ParseSetBlock(curElement);
        }
        else if(curName.equals("SetView"))
        {
            result+=ParseSetView(curElement);
        }
        else if(curName.equals("ParaList"))
        {
            result+=ParseParaList(curElement);
        }
        else if(curName.equals("return"))
        {
            result+="return\n";
        }
        else
        {
            System.out.println("Error： 出现未匹配的标签< "+ curName +" >!!!\n");
        }

        //TODO: <ReturnList>先默认一个返回值
        //TODO: ...这个先不做
        //TODO: colonExpr暂时都是小于等于两个子标签以内的情况
        //TODO: 变量类型的解决方案，Matrix有两个属性，有点小麻烦
        //TODO: 变量存活期问题
        return result;
    }

    //自定义查找函数
    private boolean arrayFind(ArrayList<String> nameFunArray, String str)
    {
        Iterator it=nameFunArray.iterator();
        while(it.hasNext())
        {
            if(it.next().equals(str))
                return true;
        }

        return false;
    }

    //第一次函数级扫描
    private void FirstTravel(Element curElement, ArrayList<Pair<String, String>> idenFunArray, ArrayList<Pair<String, String>> matFunArray,ArrayList<String> nameFunArray)
    {
        if(curElement.getName().equals("Identifier"))
        {
            if(!arrayFind(nameFunArray,curElement.getStringValue()))
            {
                Pair<String, String> temp=new Pair<String,String>(curElement.getStringValue(),curElement.attributeValue("type"));
                idenFunArray.add(temp);
                nameFunArray.add(temp.v1);
            }
        }
        else if(curElement.getName().equals("Matrix"))
        {
            if(!arrayFind(nameFunArray,curElement.getStringValue()))
            {
                Pair<String, String> temp=new Pair<>(curElement.getStringValue(),"Matrix");
                matFunArray.add(temp);
                nameFunArray.add(temp.v1);
            }
        }
        Iterator it=curElement.elementIterator();
        while(it.hasNext())
        {
            Element tempElement=(Element) it.next();
            if(!tempElement.getName().equals("ReturnList") && !tempElement.getName().equals("FunctionName")
                    &&!tempElement.getName().equals("ArgumentList"))
            {
                FirstTravel(tempElement, idenFunArray, matFunArray, nameFunArray);
            }
        }
    }

    //TODO: 以函数为入口，现在主要是怎么解决变量作用域的问题
    //TODO: 第一次扫描用深度优先遍历
    private String EntryFunctionParse(Element functionElement)
    {
        String resultValue="";
        String initSectoinStr="";
        String bingSectionStr="";
        String calcSectionStr="";
        String retSectionStr="";

        //处理函数名等结构
        Iterator itFunction = functionElement.elementIterator();
        resultValue+="//---------------------------------------\n";
        //处理函数的返回值和类型
        if(itFunction.hasNext())
        {
            Element returnListElement=(Element) itFunction.next();
            if(returnListElement.getName().equals("ReturnList"))
                resultValue+=ParseBlock(returnListElement);
            else
                System.out.println("Error: <Function>的第一个子标签不是<ReturnList>！");
        }

        //处理函数名
        if(itFunction.hasNext())
        {
            Element functionNameElement=(Element) itFunction.next();
            if(functionNameElement.getName().equals("FunctionName"))
                resultValue+=ParseBlock(functionNameElement);
            else
                System.out.println("Error: <Function>的第二个子标签不是<FunctionName>!");
        }

        //（
        resultValue+="(";

        //处理参数列表
        if(itFunction.hasNext())
        {
            Element argumentListElement=(Element) itFunction.next();
            if(argumentListElement.getName().equals("ArgumentList"))
                resultValue+=ParseBlock(argumentListElement);
            else
                System.out.println("Error: <Function>的第三个子标签不是<ArgumentList>!");
        }

        //）{
        resultValue+="){\n";

        //TODO: 第一轮扫描
        //TODO: 只有算子中的矩阵需要做绑定
        ArrayList<Pair<String,String>> idenFunArray=new ArrayList<>();
        ArrayList<Pair<String,String>> matFunArray=new ArrayList<>();
        ArrayList<String> nameFunArray=new ArrayList<>();
        //TODO: map<>


        FirstTravel(functionElement, idenFunArray, matFunArray, nameFunArray);
//        System.out.println(idenFunArray);
//        System.out.println(matFunArray);
//        System.out.println(nameFunArray);

        //TODO: init Section
        resultValue+="//Init Section\n";
        Iterator itIdenFunArray=idenFunArray.iterator();
        while(itIdenFunArray.hasNext())
        {
            Pair<String,String> temp = (Pair)itIdenFunArray.next();
            resultValue += temp.v2+" ";
            resultValue += temp.v1+";\n";
        }

        //TODO: bind Section
        //TODO: para目前还需要解决方案
        resultValue+="\n//Bind Section\n";
        Iterator itMatFunArray=matFunArray.iterator();
        int i=0;
        String para=" ";
        while(itMatFunArray.hasNext())
        {
            Pair<String,String> temp=(Pair)itMatFunArray.next();
            String tempBlk=temp.v1+"CBlk";
            String tempView=temp.v1+"CVView";
            resultValue += "SPB_COMPLEX_F *"+ tempBlk +"= NULL;\n";
            resultValue += "vsip_cvview_f *" + tempView +" = NULL;\n";
            resultValue += tempBlk + "= &pst_VsipRC->stComplexF[" + i + "];\n";
            i++;
            //Block bind
            resultValue += "iRet = setCBlockF(pfSrcDataCBlk->pfCBlock, pfSrcData);\n";
            //View bind
            //view初始化
            resultValue += tempView + "=" + tempBlk + "->pfCView[0][0];\n";
            resultValue += "iRet = setCViewF(" + tempView + "," + 0 + "," + 1 + "," + para + ");\n\n";
        }

        //TODO: calc Section
        resultValue+="//Calc Section\n";
        itFunction=functionElement.elementIterator();
        //处理函数体
        while(itFunction.hasNext())
        {
            Element functionBodyElement=(Element) itFunction.next();
            resultValue+=ParseBlock(functionBodyElement);
        }




        resultValue+="}\n\n";
        return resultValue;
    }

//    public void getC(String filePath) throws DocumentException {
//        //1.创建Reader对象
//        SAXReader reader = new SAXReader();
//        //2.加载xml
//        Document document = reader.read(new File(filePath));
//        //3.获取根节点
//        Element rootElement = document.getRootElement();
//
//        //判断根节点是否为“Program”
//        if(rootElement.getName().equals("Program"))
//        {
//            Iterator rootIterator=rootElement.elementIterator();
//            while(rootIterator.hasNext())
//            {
//                Element curElement = (Element) rootIterator.next();
//                resultProgram += ParseBlock(curElement);
//            }
//        }
//        else
//        {
//            System.out.println("Error: Root Element isn't Program.");
//        }
//        System.out.println("---------------------------------------------\n");
//        System.out.println(resultProgram);
//        System.out.println("---------------------------------------------\n");
//        System.out.println(identifierArray);
//    }

    //入口函数
    public void getC_test(String filePath) throws DocumentException {
        //1.创建Reader对象
        SAXReader reader = new SAXReader();
        //2.加载xml
        Document document = reader.read(new File(filePath));
        //3.获取根节点
        Element rootElement = document.getRootElement();

        //判断根节点是否为“Program”
        if(rootElement.getName().equals("Program"))
        {
            Iterator rootIterator=rootElement.elementIterator();
            while(rootIterator.hasNext())
            {
                Element curElement = (Element) rootIterator.next();
                resultProgram += EntryFunctionParse(curElement);
            }
        }
        else
        {
            System.out.println("Error: Root Element isn't Program.");
        }
        System.out.println("---------------------------------------------\n");
        System.out.println(resultProgram);
        System.out.println("---------------------------------------------\n");
        System.out.println(identifierArray);
    }

    //TODO: 1 所有有可能使用的变量都采用集中声明，包括临时变量
    //TODO: 2 函数分离，以函数作为入口来处理，保证不同的函数私有自己的变量集合
    //TODO: 3 将每个函数分成：（1）Init Section；（2）Bind Section；（3）Calc Section；（4）Return Section；然后再做整体输出
    //TODO: 4 Init Section 和 Bind Section 需要先扫描一遍XML文件，先把这一段解决
    //TODO: 5 最后在整个代码中加入结构体等其他格式
    //TODO: question:目前的做法不能做函数不对应的情况
    public static void main(String[] args) throws Exception {
        String filePath="./output/out.xml";
        XMLParser xmlp=new XMLParser();
        xmlp.getC_test(filePath);
    }
}
