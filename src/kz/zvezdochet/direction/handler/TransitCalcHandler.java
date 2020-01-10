package kz.zvezdochet.direction.handler;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
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
import kz.zvezdochet.core.util.CalcUtil;
import kz.zvezdochet.core.util.DateUtil;
import kz.zvezdochet.direction.part.TransitPart;
import kz.zvezdochet.service.AspectService;

/**
 * Обработчик расчёта транзитов на указанный период
 * @author Natalie Didenko
 */
public class TransitCalcHandler extends Handler {

	@Execute
	public void execute(@Active MPart activePart) {
		try {
			TransitPart periodPart = (TransitPart)activePart.getObject();
			if (!periodPart.check(0)) return;
			Event person = periodPart.getPerson();

			Place place = periodPart.getPlace();
			if (null == place)
				place = new Place().getDefault();
			double zone = periodPart.getZone();

			Collection<Planet> planets = person.getPlanets().values();
			Collection<House> houses = person.getHouses().values();
	
			updateStatus("Расчёт транзитов на период", false);

			Planet selplanet = periodPart.getPlanet();
			House selhouse = periodPart.getHouse();
			Aspect selaspect = periodPart.getAspect();
			List<Model> aspects = new ArrayList<Model>();
			if (null == selaspect)
				aspects.addAll(new AspectService().getMajorList());
			else
				aspects.add(selaspect);

			Date initDate = periodPart.getInitialDate();
			Date finalDate = periodPart.getFinalDate();
			Calendar start = Calendar.getInstance();
			start.setTime(initDate);
			Calendar end = Calendar.getInstance();
			end.setTime(finalDate);

			List<SkyPointAspect> items = new ArrayList<>();
			SimpleDateFormat sdf = new SimpleDateFormat("EEEE, d MMMM");

			for (Date date = start.getTime(); start.before(end); start.add(Calendar.DATE, 1), date = start.getTime()) {
				Event event = new Event();
				String sdate = DateUtil.formatCustomDateTime(date, "yyyy-MM-dd") + " 00:00:00";
				Date edate = DateUtil.getDatabaseDateTime(sdate);
				String pdate = sdf.format(edate);
				event.setBirth(edate);
				event.setPlace(place);
				event.setZone(zone);
				event.calc(true);

				Collection<Planet> eplanets = event.getPlanets().values();
				for (Planet eplanet : eplanets) {
					if (null == selhouse)
						for (Planet planet : planets) {
							if (selplanet != null
									&& !eplanet.getId().equals(selplanet.getId())
									&& !planet.getId().equals(selplanet.getId()))
								continue;
		
							SkyPointAspect spa = calc(eplanet, planet, aspects);
							if (null == spa)
								continue;
							spa.setDescr(pdate);
	//						((Planet)spa.getSkyPoint1()).setHouse(item.house);
							items.add(spa);
						}

					if (selplanet != null
							&& !eplanet.getId().equals(selplanet.getId()))
						continue;

					if (person.isHousable())
						for (Model model : houses) {
							if (selhouse != null && !model.getId().equals(selhouse.getId()))
								continue;

							House house = (House)model;
//							if (31 == eplanet.getId() && 158 == house.getId())
//								System.out.println(eplanet + " - " + house);

							SkyPointAspect spa = calc(eplanet, house, aspects);
							if (null == spa)
								continue;
							spa.setDescr(pdate);
							items.add(spa);
						}
				}
			}
			updateStatus("Расчёт транзитов завершён", false);
		    periodPart.setData(items);
			updateStatus("Таблица транзитов сформирована", false);
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Определение аспектного транзита между небесными точками
	 * @param point1 транзитная планета
	 * @param point2 натальная планета или дом
	 * @param selaspects выбранные аспекты
	 * @return аспект между координатами
	 */
	private SkyPointAspect calc(SkyPoint point1, SkyPoint point2, List<Model> selaspects) {
		try {
			//находим угол между точками космограммы
			double one = point1.getLongitude();
			double two = point2.getLongitude();
			double res = CalcUtil.getDifference(one, two);

			//искусственно устанавливаем нарастающую оппозицию,
			//чтобы она синхронизировалась с соответствующим ей соединением в этот день
			if (point2 instanceof House)
				if ((res >= 179 && res < 180)
						|| CalcUtil.compareAngles(one, two))
					++res;

			for (Model model : selaspects) {
				Aspect a = (Aspect)model;

				//соединения Солнца не рассматриваем
				if (a.getPlanetid() > 0)
					continue;

				if (a.isExact(res)) { 
					SkyPointAspect aspect = new SkyPointAspect();
					aspect.setSkyPoint1(point1);
					aspect.setSkyPoint2(point2);
					aspect.setScore(res);
					aspect.setAspect(a);
					aspect.setRetro(point1.isRetrograde());
					aspect.setExact(true);
					return aspect;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
}
