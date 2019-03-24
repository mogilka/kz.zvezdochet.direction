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
import kz.zvezdochet.core.service.DataAccessException;
import kz.zvezdochet.core.util.CalcUtil;
import kz.zvezdochet.core.util.DateUtil;
import kz.zvezdochet.direction.bean.PeriodItem;
import kz.zvezdochet.direction.part.TransitPart;
import kz.zvezdochet.service.AspectService;
import kz.zvezdochet.util.Configuration;
/**
 * Обработчик расчёта транзитов на указанный период
 * @author Nataly Didenko
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

//					Event prev = new Event();
//					Calendar cal = Calendar.getInstance();
//					cal.setTime(edate);
//					cal.add(Calendar.DATE, -1);
//					prev.setBirth(cal.getTime());
//					prev.setPlace(place);
//					prev.setZone(zone);
//					prev.calc(false);

				Collection<Planet> eplanets = event.getConfiguration().getPlanets().values();
				for (Planet eplanet : eplanets) {
					//транзитную Луну не рассматриваем
					if (eplanet.getCode().equals("Moon"))
						continue;

					for (Planet planet : planets) {
						PeriodItem item = calc(eplanet, planet);
						if (null == item)
							continue;
						SkyPointAspect spa = item.getPlanetAspect();
						spa.setDescr(pdate);
						items.add(spa);
					}
					for (Model model : houses) {
						House house = (House)model;
						PeriodItem item = calc(eplanet, house);
						if (null == item)
							continue;
						SkyPointAspect spa = item.getHouseAspect();
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
	 * @param point1 первая небесная точка
	 * @param point2 вторая небесная точка
	 */
	private PeriodItem calc(SkyPoint point1, SkyPoint point2) {
		try {
			//находим угол между точками космограммы
			double res = CalcUtil.getDifference(point1.getCoord(), point2.getCoord());
			AspectService service = new AspectService();

			//для домов считаем только соединения
			if (point2 instanceof House) {
				if (res < 1) {
					Aspect a = (Aspect)service.find(1L);
					PeriodItem item = new PeriodItem();
					item.aspect = a;
					item.planet = (Planet)point1;
					item.house = (House)point2;
					return item;
				}
			} else {
				//для планет определяем, является ли аспект стандартным
				List<Model> aspects = service.getMajorList();
				for (Model realasp : aspects) {
					Aspect a = (Aspect)realasp;
					if (a.isMain() && a.isExact(res)) {
						if (a.getPlanetid() > 0)
							continue;
	
						if (point1.getCode().equals(point2.getCode()))
							continue;
	
						if (a.getCode().equals("OPPOSITION") &&
								(point2.getCode().equals("Kethu") || point2.getCode().equals("Rakhu")))
							continue;
	
						PeriodItem item = new PeriodItem();
						item.aspect = a;
						item.planet = (Planet)point1;
						Planet planet2 = (Planet)point2;
						item.planet2 = planet2;
						item.house = planet2.getHouse();
	//					System.out.println(point1.getName() + " " + type.getSymbol() + " " + point2.getName());
						return item;
					}
				}
			}
		} catch (DataAccessException e) {
			e.printStackTrace();
		}
		return null;
	}
}
