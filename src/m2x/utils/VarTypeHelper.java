package m2x.utils;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class VarTypeHelper
{
    //获取变量前缀，默认是int
    @NotNull
    public static String getPrefix(String variable)
    {
        //默认长度小于3是int
        if (variable.length() < 3)
        {
            return "int";
        }
        //检查是否是长度为3的前缀
        String type = variable.substring(0, 3);
        switch (type)
        {
            case "pst":
                return "struct *";
            case "pui":
                return "unsigned int *";
            default:
                break;
        }
        //检查是否是长度为2的前缀
        type = variable.substring(0, 2);
        switch (type)
        {
            case "pf":
                return "float *";
            case "pi":
                return "int *";
            case "ui":
                return "unsigned int";
            default:
                break;
        }
        //检查是否是长度为1的前缀
        type = variable.substring(0, 1);
        switch (type)
        {
            case "f":
                return "float";
            case "i":
                return "int";
        }
        //默认返回int
        return "int";
    }

    //获取变量后缀，默认没有
    @Nullable
    public static String getPostfix(String variable)
    {
        //todo 在有标量标记的情况下，是否会影响OR
        int len = variable.length();
        if (len < 3)
        {
            return null;
        }
        //检查是否是长度为3的后缀
        String type = variable.substring(len - 3, len);
        if (type.equals("_cv") || type.equals("_cm") || type.equals("_cs"))
        {
            return type.substring(1);
        }
        //检查是否是长度为2的后缀
        type = variable.substring(len - 2, len);
        if (type.equals("_v") || type.equals("_m") || type.equals("_s"))
        {
            return type.substring(1);
        }

        return null;
    }

    public static boolean isMatrix(String variable)
    {
        String postfix = getPostfix(variable);
        if (postfix == null || postfix.equals("s") || postfix.equals("cs"))
        {
            return false;
        }
        return true;
    }

}
