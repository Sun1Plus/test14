package x2c.utils;

import org.dom4j.Element;

import java.util.Iterator;

public class OperationParser {
    private String assignmentOperation(String leftValue,String rightValue,String offset, String length)
    {
        String resultValue="";
        resultValue+="vsip_cvputoffset_f("+leftValue+","+offset+");\n";
        resultValue+="vsip_cvputlength_f("+leftValue+","+length+");\n";
        resultValue+="vsip_cvputoffset_f("+rightValue+","+offset+");\n";
        resultValue+="vsip_cvputlength_f("+rightValue+","+length+");\n";
        resultValue+="vsip_cvcopy_f_f("+leftValue+","+rightValue+");\n";

        return resultValue;
    }

    //vsip_rscvmul_f(fBackAmp, pfPhsRdCView, pfDstCView);
    //v1是标量
    private String multiplyOperation(String v1, String v2, String result, String offset, String length)
    {
        String resultValue="";
        resultValue+="vsip_cvputoffset_f("+v2+","+offset+");\n";
        resultValue+="vsip_cvputlength_f("+v2+","+length+");\n";

        resultValue+="vsip_cvputoffset_f("+result+","+offset+");\n";
        resultValue+="vsip_cvputlength_f("+result+","+length+");\n";
        resultValue+="vsip_rscvmul_f("+v1+", "+v2+", "+result+");\n";

        return resultValue;
    }

    // vsip_cvmag_f(pfSrcCView, pfSrcAmpView);  //2
    private String absOperation(String v1,String result)
    {
        String resultValue="";
        resultValue+="vsip_cvmag_f("+v1+","+result+");\n";

        return resultValue;
    }

//    public String transform(Element curElement)
//    {
//        String resultValue="";
//        String operatorType="";
//        operatorType=curElement.attributeValue("operator");
//        if(operatorType.equals("="))
//        {
//            Iterator itAssigment=curElement.elementIterator();
//            String leftS=((Element)itAssigment.next()).getStringValue();
//            String rightS=((Element)itAssigment.next()).getStringValue();
//            String offsetS=((Element)itAssigment.next()).getStringValue();
//            String lengthS=((Element)itAssigment.next()).getStringValue();
//            resultValue+=assignmentOperation(leftS,rightS,offsetS,lengthS);
//        }
//        else if(operatorType.equals("abs"))
//        {
//            Iterator itAssigment=curElement.elementIterator();
//            String v1S=((Element)itAssigment.next()).getStringValue();
//            String resultS=((Element)itAssigment.next()).getStringValue();
//
//            resultValue+=absOperation(v1S,resultS);
//        }
//        else if(operatorType.equals("*"))
//        {
//            Iterator itAssigment=curElement.elementIterator();
//            String v1S=((Element)itAssigment.next()).getStringValue();
//            String v2S=((Element)itAssigment.next()).getStringValue();
//            String resultS=((Element)itAssigment.next()).getStringValue();
//            String offsetS=((Element)itAssigment.next()).getStringValue();
//            String lengthS=((Element)itAssigment.next()).getStringValue();
//            resultValue+=multiplyOperation(v1S,v2S,resultS,offsetS,lengthS);
//        }
//
//        return resultValue;
//    }

    public String transform(Element curElement)
    {
        return "Operation;\n";
    }
}
