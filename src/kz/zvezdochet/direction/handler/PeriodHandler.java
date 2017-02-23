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
import kz.zvezdochet.direction.part.PeriodPart;
import kz.zvezdochet.part.EventPart;
import kz.zvezdochet.util.Configuration;

/**
 * Обработчик открытия транзитного периода персоны
 * @author Nataly Didenko
 *
 */
public class PeriodHandler extends Handler {
	@Inject
	private EPartService partService;

	@Execute
	public void execute(@Active MPart activePart) {
		try {
			EventPart eventPart = (EventPart)activePart.getObject();
			Event person = (Event)eventPart.getModel(EventPart.MODE_CALC, true);
			if (null == person) return;
			Configuration conf = person.getConfiguration();
			if (null == conf) return; //TODO выдавать сообщение

			updateStatus("Открытие транзитного периода", false);
			MPart part = partService.findPart("kz.zvezdochet.direction.part.period");
		    part.setVisible(true);
		    partService.showPart(part, PartState.VISIBLE);
		    PeriodPart periodPart = (PeriodPart)part.getObject();
		    periodPart.setPerson(person);
			updateStatus("Транзитный период открыт", false);
		} catch (Exception e) {
			DialogUtil.alertError(e.getMessage());
			updateStatus("Ошибка", true);
			e.printStackTrace();
		}
	}
}