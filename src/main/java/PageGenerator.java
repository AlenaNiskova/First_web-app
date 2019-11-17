import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Map;

public class PageGenerator {

    private static PageGenerator pageGen;
    private final Configuration conf;

    public static PageGenerator instance() {
        if (pageGen==null) {
            pageGen = new PageGenerator();
        }
        return pageGen;
    }

    public String getPage(String file, Map<String, Object> data) {
        Writer stream = new StringWriter();
        try {
            Template template = conf.getTemplate(File.separator+file);
            template.process(data, stream);
        } catch (IOException | TemplateException e) {
            e.printStackTrace();
        }
        return stream.toString();
    }

    private PageGenerator() {conf=new Configuration();}
}
