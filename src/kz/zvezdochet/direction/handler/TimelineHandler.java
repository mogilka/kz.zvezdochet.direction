package kz.zvezdochet.direction.handler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.inject.Inject;

import org.eclipse.e4.core.contexts.Active;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.e4.ui.workbench.modeling.EPartService.PartState;

import kz.zvezdochet.bean.Aspect;
import kz.zvezdochet.bean.Event;
import kz.zvezdochet.bean.House;
import kz.zvezdochet.bean.Planet;
import kz.zvezdochet.bean.SkyPoint;
import kz.zvezdochet.bean.SkyPointAspect;
import kz.zvezdochet.core.bean.Model;
import kz.zvezdochet.core.handler.Handler;
import kz.zvezdochet.core.service.DataAccessException;
import kz.zvezdochet.core.ui.util.DialogUtil;
import kz.zvezdochet.core.util.CalcUtil;
import kz.zvezdochet.core.util.DateUtil;
import kz.zvezdochet.direction.part.TimelinePart;
import kz.zvezdochet.direction.part.EventPart;
import kz.zvezdochet.service.AspectService;
import kz.zvezdochet.util.Configuration;

/**
 * Расчёт таймлайна события
 * @author Nataly Didenko
 *
 */
public class TimelineHandler extends Handler {
	protected List<SkyPointAspect> aged;
	private List<Model> aspects = null;

	@Inject
	private EPartService partService;

	@Execute
	public void execute(@Active MPart activePart) {
		try {
			updateStatus("Расчёт таймлайна", false);
			aged = new ArrayList<SkyPointAspect>();
			long minutes = 360;
			EventPart transitPart = (EventPart)activePart.getObject();
			Event person = transitPart.getPerson();
			Configuration conf = person.getConfiguration();
			Collection<Planet> planets = conf.getPlanets().values();

			Event event = transitPart.getModel();
			if (null == event.getDeath()) {
				String date = DateUtil.formatCustomDateTime(event.getBirth(), "yyyy-MM-dd");
				event.setDeath(DateUtil.getDateTime(date + " 23:59:59"));
			} else {
				//если момент окончания события задан, ограничиваем таймлайн длительностью события
				long time = (event.getDeath().getTime() - event.getBirth().getTime()) / 1000;
				minutes = time / 60;
			}
			Configuration conf2 = event.getConfiguration();
			Collection<Planet> planets2 = conf2.getPlanets().values();
			List<Model> houses2 = conf2.getHouses();

			//инициализируем список аспектов
			try {
				aspects = new AspectService().getMajorList();
			} catch (DataAccessException e) {
				e.printStackTrace();
			}

			for (int i = 1; i < minutes; i++) {
				List<Model> trplanets = new ArrayList<Model>();
				for (Model model: planets) {
					Planet planet = new Planet((Planet)model);
					double coord = CalcUtil.getAgedCoord(Math.abs(planet.getLongitude()), i);
					planet.setLongitude(coord);
					trplanets.add(planet);
				}
				//дирекции натальной планеты к планетам и куспидам текущей минуты
				for (Model model : trplanets) {
					Planet trplanet = (Planet)model;
					for (Model model2 : planets2) {
						Planet planet = (Planet)model2;
						calc(trplanet, planet);
					}
					for (Model model2 : houses2) {
						House house = (House)model2;
						calc(trplanet, house);
					}
				}
			}
			updateStatus("Расчёт транзитов завершён", false);

			MPart part = partService.findPart("kz.zvezdochet.direction.part.timeline");
		    part.setVisible(true);
		    partService.showPart(part, PartState.VISIBLE);
		    TimelinePart agePart = (TimelinePart)part.getObject();
		    agePart.setData(aged);
			updateStatus("Таймлайн сформирован", false);
		} catch (Exception e) {
			DialogUtil.alertError(e.getMessage());
			updateStatus("Ошибка", true);
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
			double res = CalcUtil.getDifference(point1.getLongitude(), point2.getLongitude());
	
			SkyPointAspect aspect = new SkyPointAspect();
			aspect.setSkyPoint1(point1);
			aspect.setSkyPoint2(point2);
			aspect.setScore(res);

			//определяем, является ли аспект стандартным
			for (Model realasp : aspects) {
				Aspect a = (Aspect)realasp;
				if (a.isExact(res)) {
					aspect.setAspect(a);
					aspect.setExact(true);
					break;
				}
			}
			aged.add(aspect);
		} catch (Exception e) {
			DialogUtil.alertError(point1.getNumber() + ", " + point2.getNumber());
			e.printStackTrace();
		}
	}
}
