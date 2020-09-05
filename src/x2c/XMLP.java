package x2c;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import java.io.File;
import java.util.Iterator;
import x2c.utils.XMLParser;

public class XMLP {
    private String resultProgram="";

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
    //TODO: 4_ Bind Section只处理<Operation>内出现的变量，同时维护一个map，使得之后处理<Operation>时使用_CVVIEW变量替换原变量
    //TODO: 5 最后在整个代码中加入结构体等其他格式
    //TODO: question:目前的做法不能做函数不对应的情况
    public static void main(String[] args) throws Exception {
        String filePath="./output/out.xml";
        XMLParser xmlp=new XMLParser();
        xmlp.getC_test(filePath);
    }
}
