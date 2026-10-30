package kr.co.enjoytrip.config;

import java.sql.SQLException;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import kr.co.enjoytrip.dao.UserDao;
import kr.co.enjoytrip.service.AttractionService;
import kr.co.enjoytrip.service.PasswordHasher;
import kr.co.enjoytrip.service.NewsService;

@WebListener
public class AppLifecycle implements ServletContextListener {
    private static final AttractionService ATTRACTIONS=new AttractionService();
    private static final NewsService NEWS=new NewsService();
    private static ScheduledExecutorService scheduler;

    public static AttractionService attractions(){return ATTRACTIONS;}
    public static NewsService news(){return NEWS;}

    @Override public void contextInitialized(ServletContextEvent event) {
        String id=AppConfig.get("ENJOYTRIP_ADMIN_ID");
        String email=AppConfig.get("ENJOYTRIP_ADMIN_EMAIL");
        String password=AppConfig.get("ENJOYTRIP_ADMIN_PASSWORD");
        if(!id.isEmpty()&&!email.isEmpty()&&!password.isEmpty()) {
            try {
                UserDao users=new UserDao();
                if(users.findByLogin(id)==null) users.insert(id,email,"관리자",PasswordHasher.hash(password),"ADMIN");
            } catch(SQLException e) {event.getServletContext().log("Admin bootstrap failed",e);}
        }
        scheduler=Executors.newScheduledThreadPool(2,r->{Thread t=new Thread(r,"enjoytrip-sync");t.setDaemon(true);return t;});
        if(!AppConfig.get("ENJOYTRIP_TOUR_API_KEY").isEmpty())
            scheduler.scheduleAtFixedRate(ATTRACTIONS::syncAll,Duration.ofMinutes(2).toSeconds(),Duration.ofDays(1).toSeconds(),TimeUnit.SECONDS);
        scheduler.scheduleAtFixedRate(NEWS::sync,Duration.ofMinutes(1).toSeconds(),Duration.ofDays(1).toSeconds(),TimeUnit.SECONDS);
    }

    public static void startTourSync() {
        if(scheduler!=null) scheduler.execute(ATTRACTIONS::syncAll);
    }
    public static void startNewsSync(){if(scheduler!=null)scheduler.execute(NEWS::sync);}

    @Override public void contextDestroyed(ServletContextEvent event) {
        if(scheduler!=null) scheduler.shutdownNow();
    }
}
