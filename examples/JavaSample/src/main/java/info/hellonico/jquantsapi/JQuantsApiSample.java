package info.hellonico.jquantsapi;

import clojure.lang.Keyword;
import hellonico.jquantsapi;
import org.apache.commons.jxpath.JXPathContext;

import java.util.Map;
import java.util.stream.Stream;

/**
 * Hello Jquants in java
 *
 */
public class JQuantsApiSample
{
    public static void main( String[] args )
    {

        jquantsapi api = new jquantsapi();

        String code = "24130";
        String date = "20220301";
        Map result = api.daily(code, date);

        Stream.of(result.keySet().toArray()).forEach(System.out::println);

        System.out.println(result.get("daily_quotes"));

        JXPathContext context = JXPathContext.newContext(result);
        Double open = (Double) context.getValue("/daily_quotes[1]/Open");
        System.out.printf("Quote for %s on day %s is %f\n", code, date, open);
    }
}
