package kz.zvezdochet.direction.handler;

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
				System.out.println();
				System.out.println(date);
				Event event = new Event();
				event.setBirth(date);
				event.setPlace(place);
				event.setZone(zone);
				event.calc(false);

				List<Model> eplanets = event.getConfiguration().getPlanets();
				Planet moon = null;
				for (Model model : eplanets) {
					Planet planet = (Planet)model;
					if (planet.getCode().equals("Moon")) {
						moon = planet;
						break;
					}
				}
				for (Model model : planets) {
					Planet planet = (Planet)model;
					calc(moon, planet);
				}
				for (Model model : houses) {
					House house = (House)model;
					calc(moon, house);
				}
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
