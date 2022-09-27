package info.hellonico.jquantsapi;

import hellonico.jquantsapi;
import io.quickchart.QuickChart;
import org.apache.commons.jxpath.JXPathContext;

import java.util.Map;
import java.util.stream.Stream;

/**
 * Hello Jquants in java
 *
 */
public class JQuantsApiSample {
    static final String code = "24130";

    public static void main( String[] args )
    {

        jquantsapi api = new jquantsapi();

        simple(api);

        fromTo(api);

        chartMe(api);
    }

    private static void fromTo(jquantsapi api) {
        String from = "20220301", to = "20220305";
        Map<?,?> result = api.daily(code, from, to);
        JXPathContext context = JXPathContext.newContext(result);

        System.out.println(">>"+context.selectNodes("//Open"));

    }



    private static void simple(jquantsapi api) {

        String date = "20220301";

        Map<?,?> result = api.daily(code, date);

        Stream.of(result.keySet().toArray()).forEach(System.out::println);

        System.out.println(result.get("daily_quotes"));

        JXPathContext context = JXPathContext.newContext(result);
        Double open = (Double) context.getValue("/daily_quotes[1]/Open");
        System.out.printf("Quote for %s on day %s is %f\n", code, date, open);
    }

    private static void chartMe(jquantsapi api) {
        String from = "20220301", to = "20220505";
        Map<?,?> result = api.daily(code, from, to);
        JXPathContext context = JXPathContext.newContext(result);

        QuickChart chart = new QuickChart();
        chart.setWidth(500);
        chart.setHeight(300);
        chart.setConfig("{"
                + "    type: 'line',"
                + "    data: {"
                + "        labels: "+ context.selectNodes("//Date")+","
                + "        datasets: [{"
                + "            label: 'Open',"
                + "            data: "+ context.selectNodes("//Open")
                + "        }]"
                + "    }"
                + "}"
        );

        System.out.println(chart.getUrl());
    }
}
