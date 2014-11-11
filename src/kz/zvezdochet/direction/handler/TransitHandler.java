package kz.zvezdochet.direction.handler;

import javax.inject.Inject;

import kz.zvezdochet.bean.Event;
import kz.zvezdochet.core.handler.Handler;
import kz.zvezdochet.core.ui.util.DialogUtil;
import kz.zvezdochet.direction.part.TransitPart;
import kz.zvezdochet.part.EventPart;
import kz.zvezdochet.util.Configuration;

import org.eclipse.e4.core.contexts.Active;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.ui.basic.MPart;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.e4.ui.workbench.modeling.EPartService.PartState;

/**
 * Обработчик открытия транзитов персоны
 * @author Nataly Didenko
 *
 */
public class TransitHandler extends Handler {
	@Inject
	private EPartService partService;

	@Execute
	public void execute(@Active MPart activePart) {
		try {
			EventPart eventPart = (EventPart)activePart.getObject();
			Event event = (Event)eventPart.getModel(EventPart.MODE_CALC, true);
			if (null == event) return;
			Configuration conf = event.getConfiguration();
			if (null == conf) return; //TODO выдавать сообщение

			updateStatus("Открытие списка транзитов", false);
			MPart part = partService.findPart("kz.zvezdochet.direction.part.transit");
		    part.setVisible(true);
		    partService.showPart(part, PartState.VISIBLE);
		    TransitPart transitPart = (TransitPart)part.getObject();
		    transitPart.setPerson(event);
			updateStatus("Таблица транзитов сформирована", false);
		} catch (Exception e) {
			DialogUtil.alertError(e.getMessage());
			updateStatus("Ошибка", true);
			e.printStackTrace();
		}
	}
}