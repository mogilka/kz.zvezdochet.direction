package kz.zvezdochet.direction.handler;

import javax.inject.Inject;

import org.eclipse.e4.core.contexts.Active;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.e4.ui.workbench.modeling.EPartService.PartState;

import kz.zvezdochet.bean.Event;
import kz.zvezdochet.core.handler.Handler;
import kz.zvezdochet.core.ui.util.DialogUtil;
import kz.zvezdochet.part.EventPart;

/**
 * Обработчик открытия событий персоны
 * @author Natalie Didenko
 *
 */
public class EventsHandler extends Handler {
	@Inject
	private EPartService partService;

	@Execute
	public void execute(@Active MPart activePart) {
		try {
			EventPart eventPart = (EventPart)activePart.getObject();
			Event event = (Event)eventPart.getModel(EventPart.MODE_CALC, true);
			if (null == event) return;

			updateStatus("Открытие списка транзитов", false);
			MPart part = partService.findPart("kz.zvezdochet.direction.part.events");
		    part.setVisible(true);
		    partService.showPart(part, PartState.VISIBLE);
		    kz.zvezdochet.direction.part.EventPart transitPart = (kz.zvezdochet.direction.part.EventPart)part.getObject();
		    transitPart.setPerson(event);
			updateStatus("Таблица транзитов сформирована", false);
		} catch (Exception e) {
			DialogUtil.alertError(e);
			updateStatus("Ошибка", true);
			e.printStackTrace();
		}
	}
}