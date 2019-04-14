package kz.zvezdochet.direction.handler;

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
import kz.zvezdochet.bean.Planet;
import kz.zvezdochet.bean.SkyPointAspect;
import kz.zvezdochet.core.bean.Model;
import kz.zvezdochet.core.handler.Handler;
import kz.zvezdochet.core.ui.util.DialogUtil;
import kz.zvezdochet.core.util.CalcUtil;
import kz.zvezdochet.direction.part.EventPart;
import kz.zvezdochet.part.AspectPart;
import kz.zvezdochet.service.AspectService;
import kz.zvezdochet.util.Configuration;

/**
 * Обработчик расчёта аспектов транзита
 * @author Natalie Didenko
 *
 */
public class AspectHandler extends Handler {
	@Inject
	private EPartService partService;

	@Execute
	public void execute(@Active MPart activePart) {
		try {
			EventPart synPart = (EventPart)activePart.getObject();
			Event person = synPart.getPerson();
			if (null == person) return;
			Configuration conf = person.getConfiguration();
			if (null == conf) return; //TODO выдавать сообщение
			if (null == conf.getPlanets()) return; //TODO выдавать сообщение
			updateStatus("Инициализация персоны", false);

			Event event = (Event)synPart.getModel();
			if (null == event) return;
			Configuration conf2 = event.getConfiguration();
			if (null == conf2) return; //TODO выдавать сообщение
			if (null == conf2.getPlanets()) return; //TODO выдавать сообщение
			updateStatus("Инициализация транзитного события", false);

			Collection<Planet> planets = conf.getPlanets().values();
			Collection<Planet> planets2 = conf2.getPlanets().values();
			int pcount = planets.size();
			Object[][] data = new Object[pcount][pcount + 1];
			//заполняем заголовки строк названиями планет и их координатами
			for (Planet planet : planets2)
				data[planet.getId().intValue() - 19][0] = planet.getName() + " (" + CalcUtil.roundTo(planet.getCoord(), 1) + ")";

			//формируем массив аспектов планет
			List<Model> aspects = new AspectService().getList();
			for (Planet planet : planets2) {
				for (Planet planet2 : planets) {
					double res = CalcUtil.getDifference(planet.getCoord(), planet2.getCoord());
					SkyPointAspect aspect = new SkyPointAspect();
					aspect.setSkyPoint1(planet);
					aspect.setSkyPoint2(planet2);
					aspect.setScore(CalcUtil.roundTo(res, 2));
					for (Model realasp : aspects) {
						Aspect a = (Aspect)realasp;
						if (a.isAspect(res) && a.isExact(res)) {
							aspect.setAspect(a);
							continue;
						}
					}
					data[planet.getId().intValue() - 19][planet2.getId().intValue() - 18] = aspect;
				}
			}
			updateStatus("Расчёт аспектов завершён", false);
			MPart part = partService.findPart("kz.zvezdochet.part.aspect");
		    part.setVisible(true);
		    partService.showPart(part, PartState.VISIBLE);
		    AspectPart aspectPart = (AspectPart)part.getObject();
		    aspectPart.setConfiguration(conf);
		    aspectPart.setData(data);
			updateStatus("Таблица аспектов сформирована", false);
		} catch (Exception e) {
			DialogUtil.alertError(e.getMessage());
			updateStatus("Ошибка", true);
			e.printStackTrace();
		}		
	}		
}
