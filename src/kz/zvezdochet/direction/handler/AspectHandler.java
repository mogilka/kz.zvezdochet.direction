package kz.zvezdochet.direction.handler;

import java.util.List;

import javax.inject.Inject;

import kz.zvezdochet.bean.Aspect;
import kz.zvezdochet.bean.Event;
import kz.zvezdochet.bean.Planet;
import kz.zvezdochet.bean.SkyPointAspect;
import kz.zvezdochet.core.bean.Model;
import kz.zvezdochet.core.handler.Handler;
import kz.zvezdochet.core.ui.util.DialogUtil;
import kz.zvezdochet.core.util.CalcUtil;
import kz.zvezdochet.direction.part.TransitPart;
import kz.zvezdochet.part.AspectPart;
import kz.zvezdochet.service.AspectService;
import kz.zvezdochet.util.Configuration;

import org.eclipse.e4.core.contexts.Active;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.e4.ui.workbench.modeling.EPartService.PartState;

/**
 * Обработчик расчёта аспектов транзита
 * @author Nataly Didenko
 *
 */
public class AspectHandler extends Handler {
	@Inject
	private EPartService partService;

	@Execute
	public void execute(@Active MPart activePart) {
		try {
			TransitPart synPart = (TransitPart)activePart.getObject();
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

			List<Model> planets = conf.getPlanets();
			List<Model> planets2 = conf2.getPlanets();
			int pcount = planets.size();
			Object[][] data = new Object[pcount][pcount + 1];
			//заполняем заголовки строк названиями планет и их координатами
			for (int i = 0; i < pcount; i++) {
				Planet planet = (Planet)planets2.get(i);
				data[i][0] = planet.getName() + " (" + CalcUtil.roundTo(planet.getCoord(), 1) + ")";
			}

			//формируем массив аспектов планет
			List<Model> aspects = new AspectService().getList();
			for (int c = 0; c < pcount; c++) {
				Planet planet = (Planet)planets.get(c);
				for (int r = 0; r < pcount; r++) {
					Planet planet2 = (Planet)planets2.get(r);
					double res = CalcUtil.getDifference(planet.getCoord(), planet2.getCoord());
					SkyPointAspect aspect = new SkyPointAspect();
					aspect.setSkyPoint1(planet);
					aspect.setSkyPoint2(planet2);
					aspect.setScore(CalcUtil.roundTo(res, 2));
					for (Model realasp : aspects) {
						Aspect a = (Aspect)realasp;
						if (a.isAspect(res)) {
							aspect.setAspect(a);
							continue;
						}
					}
					data[r][c + 1] = aspect;
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
