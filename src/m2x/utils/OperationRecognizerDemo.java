package m2x.utils;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;
import static m2x.utils.VarTypeHelper.getPostfix;

public class OperationRecognizerDemo
{
    final private static List<String> MATLABFunctionNames = new ArrayList<>();
    final private static List<String> userDefinedFunctionName = new ArrayList<>();
    final private static OperationRecognizerDemo or = new OperationRecognizerDemo();

    //加载文件内容
    private OperationRecognizerDemo()
    {
        try
        {
            String filePath = "./input/changeable_type.txt";
            Scanner scanner = new Scanner(new File(filePath));
            while (scanner.hasNextLine())
            {
                MATLABFunctionNames.add(scanner.nextLine().split("%")[0].trim());
            }
            filePath = "./input/fixed_type.txt";
            scanner = new Scanner(new File(filePath));
            while (scanner.hasNextLine())
            {
                MATLABFunctionNames.add(scanner.nextLine().split("%")[0].trim());
            }
            filePath = "./input/function_names.txt";
            scanner = new Scanner(new File(filePath));
            while (scanner.hasNextLine())
            {
                userDefinedFunctionName.add(scanner.nextLine().trim());
            }
        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();
        }
    }

    public static OperationRecognizerDemo getInstance()
    {
        return or;
    }

    private boolean hasUserDefinedFunction(String line)
    {
        TokenStream tokenStream = new TokenStream(line);
        while (tokenStream.nextToken() != null)
        {
            String token = tokenStream.getToken();
            for (String f : userDefinedFunctionName)
            {
                if (f.equals(token))
                {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isFunctionName(String token)
    {
        for (String s : MATLABFunctionNames)
        {
            if (s.equals(token))
            {
                return true;
            }
        }

        return false;
    }

    //判断是否是算子：
    //存在MATLAB内函数调用的
    //出现矢量时，只有向量取单个值视为非算子，其他出现矢量的情况一律视为算子
    @SuppressWarnings("ConstantConditions")
    public boolean isOperation(String line)
    {
        if (hasUserDefinedFunction(line))
        {
            return false;
        }
        TokenStream tokenStream = new TokenStream(line);
        String token;
        while (tokenStream.nextToken() != null)
        {
            token = tokenStream.getToken();
            if (Character.isLetter(token.charAt(0)))
            {
                if (isFunctionName(token))
                {
                    return true;
                }
                String postfix = getPostfix(token);
                if (postfix != null)
                {
                    if (postfix.equals("m") || postfix.equals("cm"))
                    {
                        return true;
                    }
                    else if(postfix.equals("v") || postfix.equals("cv"))
                    {
                        String s = tokenStream.nextToken();
                        if (s != null && s.equals("(")) //在向量的情况下，只能是取单个值
                        {
                            s = tokenStream.getToken();
                            while (!s.equals(")"))
                            {
                                if (s.equals(":"))
                                {
                                    return true;
                                }
                                s = tokenStream.getToken();
                            }
                        }
                    }
                }
            }
        }

        return false;
    }
}
