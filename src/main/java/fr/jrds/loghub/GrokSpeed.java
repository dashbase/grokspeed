package fr.jrds.loghub;

import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;

import io.thekraken.grok.api.Match;
import io.thekraken.grok.api.exception.GrokException;

@State(Scope.Thread)
public class GrokSpeed {

    private io.thekraken.grok.api.Grok grok1;
    private io.thekraken.grok.api.Grok grok2;
    private Pattern syslog;

    @Setup
    public void prepare() throws GrokException {
        grok1 = new io.thekraken.grok.api.Grok();
        grok1.addPattern("NONNEGINT", "\\b(?:[0-9]+)\\b");
        grok1.addPattern("GREEDYDATA", ".*");
        grok1.compile("^<%{NONNEGINT:syslog_pri}>%{GREEDYDATA:message}", false);
        
        grok2 = new io.thekraken.grok.api.Grok();
        grok2.compile("<(?<syslogpri>\\b(?:[0-9]+)\\b)>(?<message>.*)", false);
        
        syslog = Pattern.compile("<(?<syslogpri>\\b(?:[0-9]+)\\b)>(?<message>.*)");
    }

    @Benchmark
    public Match grokSpeed() {
        Match gm = grok1.match("<1>totor");
        gm.captures();
        Map<String, Object> mapped = gm.toMap();
        assert mapped.get("syslog_pri") != null;
        assert mapped.get("message") != null;
        return gm;
    }

    @Benchmark
    public Match notrealgrokSpeed() {
        Match gm = grok2.match("<1>totor");
        gm.captures();
        Map<String, Object> mapped = gm.toMap();
        assert mapped.get("syslogpri") != null;
        assert mapped.get("message") != null;
        return gm;
    }

    @Benchmark
    public Matcher javaRegexSpeed() {
        Matcher m = syslog.matcher("<1>totor");
        m.matches();
        assert m.group("syslogpri") != null;
        assert m.group("message") != null;
        return m;
    }

    public static void main(String[] args) throws RunnerException {

        Options opt = new OptionsBuilder()
                // Specify which benchmarks to run. 
                // You can be more specific if you'd like to run only one benchmark per test.
                .include(GrokSpeed.class.getName() + ".*")
                // Set the following options as needed
                .mode (Mode.AverageTime)
                .timeUnit(TimeUnit.MICROSECONDS)
                .warmupTime(TimeValue.seconds(1))
                .warmupIterations(10)
                .measurementTime(TimeValue.seconds(10))
                .measurementIterations(5)
                .threads(1)
                .forks(1)
                .shouldFailOnError(true)
                .shouldDoGC(true)
                //.jvmArgs("-XX:+UnlockDiagnosticVMOptions", "-XX:+PrintInlining")
                //.addProfiler(WinPerfAsmProfiler.class)
                .build();

        new Runner(opt).run();
    }

}
