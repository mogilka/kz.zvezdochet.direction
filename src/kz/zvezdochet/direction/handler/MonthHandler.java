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
import kz.zvezdochet.direction.part.MonthPart;
import kz.zvezdochet.direction.part.TransitPart;

/**
 * Обработчик отображения представления транзитов месяца
 * @author Natalie Didenko
 *
 */
public class MonthHandler extends Handler {
	@Inject
	private EPartService partService;

	@Execute
	public void execute(@Active MPart activePart) {
		try {
			TransitPart eventPart = (TransitPart)activePart.getObject();
			Event event = (Event)eventPart.getPerson();
			if (null == event) return;
		
			MPart part = partService.findPart("kz.zvezdochet.direction.part.month");
		    part.setVisible(true);
		    partService.showPart(part, PartState.VISIBLE);
		    MonthPart agePart = (MonthPart)part.getObject();
		    if (agePart != null)
		    	agePart.setPerson(event);
		} catch (Exception e) {
			DialogUtil.alertError(e);
			e.printStackTrace();
		}
	}
}