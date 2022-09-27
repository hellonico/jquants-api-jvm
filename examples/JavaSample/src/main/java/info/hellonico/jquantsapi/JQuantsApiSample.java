package info.hellonico.jquantsapi;

import com.github.signaflo.timeseries.TimeSeries;
import hellonico.jquantsapi;
import io.quickchart.QuickChart;
import org.apache.commons.jxpath.JXPathContext;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static java.lang.String.*;

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

        listedInfoAndStatement(api);

        fromTo(api);

        chartMe(api);

        movingAverage(api);

        System.exit(0);
    }

    private static void listedInfoAndStatement(jquantsapi api) {
        Map<?,?> listedInfo = api.listedInfo(code);
        JXPathContext context = JXPathContext.newContext(listedInfo);
        System.out.printf("Code %s is for companyName %s\n", code, context.getValue("//CompanyNameFull"));

        Map<?,?> statements = api.statements(code, "20220727");
        JXPathContext context2 = JXPathContext.newContext(statements);
        System.out.printf("Profit: %s for Code %s\n", context2.getValue("//Profit"), code);
    }

    public static List<Double> pad(List<Double> array, int pad){
        LinkedList<Double> list = new LinkedList();
        list.addAll(array);
        for(int i = 0;i<pad;i++) {
            list.add(0,null);
        }
        return list;
    }

    private static void movingAverage(jquantsapi api) {
        String from = "20220301", to = "20220401";
        Map<?,?> result = api.daily(code, from, to);
        JXPathContext context = JXPathContext.newContext(result);

        double[] tds = context.selectNodes("//Open").stream().mapToDouble(p -> (Double) p).toArray();
        TimeSeries ts = TimeSeries.from(tds);
        int MOVING_AVERAGE_RANGE = 5;
        TimeSeries ma = ts.movingAverage(MOVING_AVERAGE_RANGE);

        QuickChart chart = new QuickChart();
        chart.setWidth(500);
        chart.setHeight(500);

        String config =
                format("{type: 'line',data: {labels: %s , datasets: [{label: 'MovingAverage', data:%s ,fill: false}]}}",
                        context.selectNodes("//Date"),
                        pad(ma.asList(),MOVING_AVERAGE_RANGE--)
                        );
        chart.setConfig(config);

        System.out.println(chart.getUrl());

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
        // https://quickchart.io/documentation/#library-java
        String from = "20220301", to = "20220505";
        Map<?,?> result = api.daily(code, from, to);
        JXPathContext context = JXPathContext.newContext(result);

        QuickChart chart = new QuickChart();
        chart.setWidth(500);
        chart.setHeight(300);
        String config =
                format("{type: 'line',data: {labels: %s , datasets: [{label: 'Open', data:%s ,fill: false}, {label: 'Close', data:%s ,fill: false}]}}",
                        context.selectNodes("//Date"),
                        context.selectNodes("//Open"),
                        context.selectNodes("//Close"));
        chart.setConfig(config);

        System.out.println(chart.getUrl());
    }
}
