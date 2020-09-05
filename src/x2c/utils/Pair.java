package x2c.utils;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class Pair<T1, T2>
{
    public T1 v1;
    public T2 v2;

    public Pair(T1 v1, T2 v2)
    {
        this.v1 = v1;
        this.v2 = v2;
    }

    @Override
    public String toString() {
        return "["+this.v1.toString()+", "+this.v2.toString()+"]";
    }



    public static void main(String[] args)
    {
        List<String> m = new ArrayList<>();
        System.out.println(m.toString());
    }
}
