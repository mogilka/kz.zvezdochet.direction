package kz.zvezdochet.direction.handler;

import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.model.application.ui.advanced.MPerspective;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.e4.ui.workbench.modeling.EPartService;

/**
 * Обработчик открытия раздела прогнозов
 * @author Nataly Didenko
 *
 */
public class PredictionsHandler {
	@Execute
	public void execute(MApplication app, EModelService service, EPartService partService) {
		MPerspective perspective = (MPerspective)service.find("kz.zvezdochet.direction.perspective.prediction", app);
		if (perspective != null)
			partService.switchPerspective(perspective);
	}
}