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
import kz.zvezdochet.direction.bean.PeriodItem;
import kz.zvezdochet.direction.part.TransitPart;
import kz.zvezdochet.util.Configuration;
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

			Configuration conf = person.getConfiguration();
			Collection<Planet> planets = conf.getPlanets().values();
			List<Model> houses = conf.getHouses();
	
			updateStatus("Расчёт транзитов на период", false);

			Planet selplanet = periodPart.getPlanet();
			House selhouse = periodPart.getHouse();
			Aspect selaspect = periodPart.getAspect();

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
				event.calc(false);

				Collection<Planet> eplanets = event.getConfiguration().getPlanets().values();
				for (Planet eplanet : eplanets) {
					//транзитную Луну не рассматриваем
					if (eplanet.getCode().equals("Moon"))
						continue;

					if (null == selhouse)
						for (Planet planet : planets) {
							if (selplanet != null && !planet.getId().equals(selplanet.getId()))
								continue;
	
							PeriodItem item = calc(eplanet, planet, selaspect);
							if (null == item)
								continue;
							SkyPointAspect spa = item.getPlanetAspect();
							spa.setAspect(selaspect);
							spa.setDescr(pdate);
							spa.setRetro(eplanet.isRetrograde());
							((Planet)spa.getSkyPoint1()).setHouse(item.house);
							items.add(spa);
						}

					if (selplanet != null && selaspect != null) {
						//
					} else if (person.isHousable())
						for (Model model : houses) {
							if (selhouse != null && !model.getId().equals(selhouse.getId()))
								continue;
	
							House house = (House)model;
							PeriodItem item = calc(eplanet, house, selaspect);
							if (null == item)
								continue;
							SkyPointAspect spa = item.getHouseAspect();
							spa.setAspect(selaspect);
							spa.setDescr(pdate);
							spa.setRetro(eplanet.isRetrograde());
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
	 * @param aspect аспект
	 */
	private PeriodItem calc(SkyPoint point1, SkyPoint point2, Aspect aspect) {
		try {
			//находим угол между точками космограммы
			double res = CalcUtil.getDifference(point1.getLongitude(), point2.getLongitude());

			if (aspect.isExact(res)) {
				PeriodItem item = new PeriodItem();
				item.aspect = aspect;
				item.planet = (Planet)point1;
				if (point2 instanceof House) {
					item.house = (House)point2;
				} else if (0 == aspect.getPlanetid()) {
					Planet planet2 = (Planet)point2;
					item.planet2 = planet2;
					item.house = planet2.getHouse();
				}
				return item;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
}
