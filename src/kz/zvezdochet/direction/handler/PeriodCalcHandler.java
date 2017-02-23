package kz.zvezdochet.direction.handler;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.eclipse.e4.core.contexts.Active;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;

import kz.zvezdochet.bean.Aspect;
import kz.zvezdochet.bean.Event;
import kz.zvezdochet.bean.House;
import kz.zvezdochet.bean.Place;
import kz.zvezdochet.bean.Planet;
import kz.zvezdochet.bean.SkyPoint;
import kz.zvezdochet.bean.SkyPointAspect;
import kz.zvezdochet.core.bean.Model;
import kz.zvezdochet.core.handler.Handler;
import kz.zvezdochet.core.service.DataAccessException;
import kz.zvezdochet.core.util.CalcUtil;
import kz.zvezdochet.core.util.DateUtil;
import kz.zvezdochet.direction.part.PeriodPart;
import kz.zvezdochet.service.AspectService;
import kz.zvezdochet.util.Configuration;

/**
 * Обработчик расчёта транзитов на указанный период
 * @author Nataly Didenko
 */
public class PeriodCalcHandler extends Handler {

	@Execute
	public void execute(@Active MPart activePart) {
		try {
			PeriodPart periodPart = (PeriodPart)activePart.getObject();
				if (!periodPart.check(0)) return;
			Event person = periodPart.getPerson();
			Place place = periodPart.getPlace();
			double zone = periodPart.getZone();
	
			Configuration conf = person.getConfiguration();
			List<Model> planets = conf.getPlanets();
			List<Model> houses = conf.getHouses();
	
			updateStatus("Расчёт транзитов на период", false);
	
			Date initDate = periodPart.getInitialDate();
			Date finalDate = periodPart.getFinalDate();
			Calendar start = Calendar.getInstance();
			start.setTime(initDate);
			Calendar end = Calendar.getInstance();
			end.setTime(finalDate);

			for (Date date = start.getTime(); start.before(end); start.add(Calendar.DATE, 1), date = start.getTime()) {
				System.out.println(date);
				for (int i = 1; i < 5; i++) {
					int h = i * 6;
					String shour = (h < 10) ? "0" + h : String.valueOf(h);
					String sdate = DateUtil.formatCustomDateTime(date, "yyyy-MM-dd") + " " + shour + ":00:00";
					System.out.println(shour);

					Event event = new Event();
					Date edate = DateUtil.getDatabaseDateTime(sdate);
					event.setBirth(edate);
					event.setPlace(place);
					event.setZone(zone);
					event.calc(true);

					Event prev = new Event();
					Calendar cal = Calendar.getInstance();
					cal.setTime(edate);
					cal.add(Calendar.DATE, -1);
					prev.setBirth(cal.getTime());
					prev.setPlace(place);
					prev.setZone(zone);
					prev.calc(false);

					List<Planet> iplanets = new ArrayList<Planet>();
					List<Model> eplanets = event.getConfiguration().getPlanets();
					for (Model model : eplanets) {
						Planet planet = (Planet)model;
						List<Object> ingresses = planet.isIngressed(prev, event);
						if (ingresses != null && ingresses.size() > 0)
							iplanets.add(planet);
					}

					for (Planet eplanet : iplanets) {
						for (Model model : planets) {
							Planet planet = (Planet)model;
							calc(eplanet, planet);
						}
						for (Model model : houses) {
							House house = (House)model;
							calc(eplanet, house);
						}
					}
				}
				System.out.println();
			}	
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Определение аспектной дирекции между небесными точками
	 * @param point1 первая небесная точка
	 * @param point2 вторая небесная точка
	 */
	private void calc(SkyPoint point1, SkyPoint point2) {
		try {
			//находим угол между точками космограммы
			double res = CalcUtil.getDifference(point1.getCoord(), point2.getCoord());
	
			//определяем, является ли аспект стандартным
			List<Model> aspects = new AspectService().getList();
			for (Model realasp : aspects) {
				Aspect a = (Aspect)realasp;
				if (a.getTypeid() < 4 && a.isExactTruncAspect(res)) {
					SkyPointAspect aspect = new SkyPointAspect();
					aspect.setSkyPoint1(point1);
					aspect.setSkyPoint2(point2);
					aspect.setScore(res);
					aspect.setAspect(a);
					System.out.println(aspect);
				}
			}
		} catch (DataAccessException e) {
			e.printStackTrace();
		}
	}
}
