package kz.zvezdochet.direction.handler;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.eclipse.e4.core.contexts.Active;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;

import kz.zvezdochet.bean.Event;
import kz.zvezdochet.bean.House;
import kz.zvezdochet.bean.Place;
import kz.zvezdochet.bean.Planet;
import kz.zvezdochet.bean.SkyPoint;
import kz.zvezdochet.bean.SkyPointAspect;
import kz.zvezdochet.core.handler.Handler;
import kz.zvezdochet.core.ui.util.DialogUtil;
import kz.zvezdochet.core.util.DateUtil;
import kz.zvezdochet.direction.part.TransitPart;

/**
 * Расчёт транзитов месяца
 * @author Natalie Didenko
 */
public class MonthHandler extends Handler {

	@Execute
	public void execute(@Active MPart activePart) {
		try {
			updateStatus("Расчёт транзитов месяца", false);
			TransitPart transitPart = (TransitPart)activePart.getObject();
			Event person = (Event)transitPart.getPerson();
			if (null == person) return;

			Place place = transitPart.getPlace();
			transitPart.setMode(TransitPart.MODE_TABLE);

			Collection<Planet> planets = person.getPlanets().values();
			int dcount = 32; //число дней в месяце
			int pcount = planets.size();
			String[][] data = new String[pcount][dcount];
			String[][] data2 = new String[pcount][dcount];
			//заполняем заголовки строк названиями планет
			for (Planet planet : planets) {
				data[planet.getNumber() - 1][0] = planet.getSymbol();
				data2[planet.getNumber() - 1][0] = planet.getSymbol();
			}

			Calendar start = Calendar.getInstance();
			start.setTime(transitPart.getInitialDate());
			Calendar end = Calendar.getInstance();
			end.setTime(transitPart.getFinalDate());
			List<Date> dates = new ArrayList<Date>(); 
			for (Date date = start.getTime(); start.before(end); start.add(Calendar.DATE, 1), date = start.getTime())
				dates.add(date);

			for (Date date : dates) {
				Calendar calendar = Calendar.getInstance();
				calendar.setTime(date);
				int day = calendar.get(Calendar.DAY_OF_MONTH);

				String sdate = DateUtil.formatCustomDateTime(date, "yyyy-MM-dd") + " 12:00:00";
				Event event = new Event();
				Date edate = DateUtil.getDatabaseDateTime(sdate);
				event.setBirth(edate);
				event.setPlace(place);
				event.setZone(place.getZone());
				event.calc(true);

				Map<String, List<Object>> ingressList = person.initIngresses(event);
				if (ingressList.isEmpty())
					continue;

				for (Map.Entry<String, List<Object>> daytexts : ingressList.entrySet()) {
					List<Object> objects = daytexts.getValue();
					if (objects.isEmpty())
						continue;

					for (Object object : objects) {
						if (object instanceof SkyPointAspect) {
							SkyPointAspect spa = (SkyPointAspect)object;
							String acode = spa.getAspect().getCode();
							Planet planet = (Planet)spa.getSkyPoint1();
							SkyPoint skyPoint = spa.getSkyPoint2();

							boolean housable = skyPoint instanceof House;
    		                if (acode.equals("OPPOSITION")) {
    		                	if (planet.getCode().equals("Rakhu")
    		                			|| planet.getCode().equals("Kethu"))
    		                		continue;
    		                	if (!housable)
	    		                	if (skyPoint.getCode().equals("Rakhu")
	    		                			|| skyPoint.getCode().equals("Kethu"))
	    		                		continue;
    		                }
		                	String value = spa.getSymbol();
    		                if (housable) {
        		                if (planet.getCode().equals("Moon")
        		                		&& !acode.equals("CONJUNCTION"))
        		                	continue;

    		                	String text = data2[planet.getNumber() - 1][day];
    		                	data2[planet.getNumber() - 1][day] = (null == text) ? value : text + "\n" + value;
    		                } else {
    		                	String text = data[planet.getNumber() - 1][day];
    		                	data[planet.getNumber() - 1][day] = (null == text) ? value : text + "\n" + value;
    					        if (28 == planet.getId() && 21 == skyPoint.getId())
    					        	System.out.println(day + " " + data[planet.getNumber() - 1][day]);
    		                }
						}
					}
				}
			}
			updateStatus("Расчёт транзитов завершён", false);
			transitPart.setData(data);
			transitPart.setData2(data2);
			updateStatus("Таблица транзитов сформирована", false);
		} catch (Exception e) {
			DialogUtil.alertError(e);
			updateStatus("Ошибка", true);
			e.printStackTrace();
		}
	}
}
