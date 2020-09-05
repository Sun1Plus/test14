package m2x.utils;

import org.jetbrains.annotations.*;
/*
 * 这个类用于将matlab语句分解为标识符
 * 辅助MATLABParser
 * */
public class TokenStream
{
    final private String oldLine;         //未处理过的语句
    private String line;            //处理到当前的语句
    final String[] mulOp =
            {
                    "==", ">=", "<=",
                    "~=", "&&", "||",
                    "./", ".*", ".^",
                    "%#"
            };                      //复合操作符

    public TokenStream(@NotNull String line)
    {
        oldLine = line.trim();
        this.line = oldLine;
    }

    //返回标识符
    @NotNull
    private String getIdentifier()
    {
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < line.length(); i++)
        {
            char ch = line.charAt(i);
            if (Character.isLetterOrDigit(ch) || ch == '_')
            {
                s.append(ch);
            }
            else
            {
                break;
            }
        }
        line = line.replaceFirst(s.toString(), "");
        return s.toString();
    }

    //返回常数
    @NotNull
    private String getConstant()
    {
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < line.length(); i++)
        {
            char ch = line.charAt(i);
            if (Character.isDigit(ch) || ch == '.')
            {
                s.append(ch);
            }
            else
            {
                break;
            }
        }
        line = line.replaceFirst(s.toString(), "");
        return s.toString();
    }

    //返回操作符
    @NotNull
    private String getOperator()
    {
        if (line.length() > 1)
        {
            String op = line.substring(0, 2);
            for (String s : mulOp)
            {
                if (s.equals(op))
                {
                    line = line.substring(2);
                    return op;
                }
            }
        }
        String op = line.substring(0, 1);
        line = line.substring(1);
        return op;
    }

    //返回一个Token
    //会改变当前的line，这样下一次调用该函数获得新的Token
    @Nullable
    public String getToken()
    {
        line = line.trim();
        if (line.equals(""))
        {
            return null;
        }

        //很明显，这里没有错误处理
        if (Character.isLetter(line.charAt(0)))
        {
            return getIdentifier();
        }
        else if (Character.isDigit(line.charAt(0)))
        {
            return getConstant();
        }
        else
        {
            return getOperator();
        }
    }

    //查看下一个Token
    //不改变当前的line，用于查看下一个Token以决定接下来的操作
    @Nullable
    public String nextToken()
    {
        String s = line;
        String r = getToken();
        line = s;
        return r;
    }

    @Deprecated //并没有需要用到这个函数的地方
    public String getOldLine()
    {
        return oldLine;
    }

    public String getLine()
    {
        return line;
    }
}