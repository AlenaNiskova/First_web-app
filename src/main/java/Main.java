import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import javax.swing.*;
import java.awt.*;
import java.net.URL;

public class Main extends JFrame {

    public Main() {
        Container c = getContentPane();
        c.setLayout(new BorderLayout());
        JPanel child= new JPanel();
        c.add(child);

        BoxLayout layoutMgr = new BoxLayout(child, BoxLayout.PAGE_AXIS);
        child.setLayout(layoutMgr);

        ImageIcon imageIcon = new ImageIcon("working.gif");
        JLabel iconLabel = new JLabel();
        iconLabel.setIcon(imageIcon);
        imageIcon.setImageObserver(iconLabel);
        child.add(iconLabel);

        JLabel label = new JLabel("Server is working...");
        child.add(label);
        label = new JLabel("Close the window to stop.");
        child.add(label);

        setTitle("Homework");
        setPreferredSize(new Dimension(600, 425));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        pack();
        setVisible(true);
    }

    public static void main(String[] args) throws Exception {
        new Main();
        Front front = new Front();

        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.addServlet(new ServletHolder(front), "/*");

        Server server = new Server(8080);
        server.setHandler(context);
        server.start();
        server.join();
    }
}
