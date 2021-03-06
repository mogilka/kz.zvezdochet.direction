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
import kz.zvezdochet.direction.part.AgePart;
import kz.zvezdochet.part.EventPart;

/**
 * Обработчик отображения представления дирекций на указанный возраст
 * @author Natalie Didenko
 *
 */
public class AgeHandler extends Handler {
	@Inject
	private EPartService partService;

	@Execute
	public void execute(@Active MPart activePart) {
		try {
			EventPart eventPart = (EventPart)activePart.getObject();
			Event event = (Event)eventPart.getModel(EventPart.MODE_ASPECT_PLANET_PLANET, true);
			if (null == event) return;
		
			MPart part = partService.findPart("kz.zvezdochet.direction.part.age");
		    part.setVisible(true);
		    partService.showPart(part, PartState.VISIBLE);
		    AgePart agePart = (AgePart)part.getObject();
		    agePart.setEvent(event);
		} catch (Exception e) {
			DialogUtil.alertError(e);
			e.printStackTrace();
		}
	}
		
}