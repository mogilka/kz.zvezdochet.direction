package kz.zvezdochet.direction.part;

import java.util.Date;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.eclipse.jface.bindings.keys.KeyStroke;
import org.eclipse.jface.fieldassist.ContentProposalAdapter;
import org.eclipse.jface.fieldassist.IContentProposal;
import org.eclipse.jface.fieldassist.IContentProposalListener;
import org.eclipse.jface.fieldassist.TextContentAdapter;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.nebula.widgets.cdatetime.CDT;
import org.eclipse.nebula.widgets.cdatetime.CDateTime;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import kz.zvezdochet.bean.Event;
import kz.zvezdochet.bean.Place;
import kz.zvezdochet.core.bean.Model;
import kz.zvezdochet.core.ui.decoration.InfoDecoration;
import kz.zvezdochet.core.ui.util.DialogUtil;
import kz.zvezdochet.core.ui.view.ModelListView;
import kz.zvezdochet.core.ui.view.View;
import kz.zvezdochet.core.util.CalcUtil;
import kz.zvezdochet.core.util.DateUtil;
import kz.zvezdochet.part.Messages;
import kz.zvezdochet.provider.PlaceProposalProvider;
import kz.zvezdochet.provider.PlaceProposalProvider.PlaceContentProposal;

/**
 * Представление транзитного периода
 * @author Nataly Didenko
 *
 */
public class PeriodPart extends ModelListView {
	@Inject
	public PeriodPart() {}

	private Event person;
	private Place trplace;

	private CDateTime dt;
	private CDateTime dt2;
	private Text txPlace;
	private Text txLatitude;
	private Text txLongitude;
	private Text txZone;
	private Text txGreenwich;

	public void setPerson(Event person) {
		this.person = person;
	}
	
	@PostConstruct @Override
	public View create(Composite parent) {
		super.create(parent);
		return null;
	}

	@Override
	public boolean check(int mode) throws Exception {
		if (null == dt.getSelection() || null == dt2.getSelection()) {
			DialogUtil.alertError("Укажите правильный период");
			return false;
		} else if (!DateUtil.isDateRangeValid(dt.getSelection(), dt2.getSelection())) {
			DialogUtil.alertError("Укажите правильный период");
			return false;
		} else if (null == trplace) {
			DialogUtil.alertError(Messages.getString("EventView.PlaceIsWrong"));
			return false;
		} else if ("" == txZone.getText()) {
			DialogUtil.alertError("Укажите правильную зону");
			return false;
		} else if (null == person) {
			DialogUtil.alertError("Событие не задано");
			return false;
		} else if (null == person.getConfiguration()) {
			DialogUtil.alertError("Конфигурация события не задана");
			return false;
		}
		return true;
	}

	@Override
	protected void init(Composite parent) {
		GridLayoutFactory.swtDefaults().numColumns(2).applyTo(parent);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(parent);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(container);
		GridLayoutFactory.swtDefaults().applyTo(container);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true, true).applyTo(table);
	}
	
	@Override
	public void initFilter() {
		grFilter = new Group(container, SWT.NONE);
		grFilter.setText("Период");

		Label lb = new Label(grFilter, SWT.NONE);
		lb.setText("Начало");
		dt = new CDateTime(grFilter, CDT.BORDER | CDT.COMPACT | CDT.DROP_DOWN | CDT.DATE_LONG | CDT.DATE_MEDIUM);
		dt.setNullText(""); //$NON-NLS-1$

		lb = new Label(grFilter, SWT.NONE);
		lb.setText("Конец");
		dt2 = new CDateTime(grFilter, CDT.BORDER | CDT.COMPACT | CDT.DROP_DOWN | CDT.DATE_LONG | CDT.DATE_MEDIUM);
		dt2.setNullText(""); //$NON-NLS-1$

		Group secPlace = new Group(grFilter, SWT.NONE);
		secPlace.setText(Messages.getString("PersonView.Place")); //$NON-NLS-1$
		txPlace = new Text(secPlace, SWT.BORDER);
		new InfoDecoration(txPlace, SWT.TOP | SWT.LEFT);

		lb = new Label(secPlace, SWT.NONE);
		lb.setText(Messages.getString("PersonView.Latitude")); //$NON-NLS-1$
		txLatitude = new Text(secPlace, SWT.BORDER);
		txLatitude.setEditable(false);

		lb = new Label(secPlace, SWT.NONE);
		lb.setText(Messages.getString("PersonView.Longitude")); //$NON-NLS-1$
		txLongitude = new Text(secPlace, SWT.BORDER);
		txLongitude.setEditable(false);

		lb = new Label(secPlace, SWT.NONE);
		lb.setText(Messages.getString("PersonView.Greenwith")); //$NON-NLS-1$
		txGreenwich = new Text(secPlace, SWT.BORDER);
		txGreenwich.setEditable(false);

		lb = new Label(secPlace, SWT.NONE);
		lb.setText(Messages.getString("PersonView.Zone")); //$NON-NLS-1$
		txZone = new Text(secPlace, SWT.BORDER);

		GridLayoutFactory.swtDefaults().numColumns(10).applyTo(grFilter);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(grFilter);
		GridLayoutFactory.swtDefaults().numColumns(4).applyTo(secPlace);
		GridDataFactory.fillDefaults().span(4, 1).grab(true, false).applyTo(secPlace);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).
			grab(true, false).applyTo(dt);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).
			grab(true, false).applyTo(dt2);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).
			span(4, 1).grab(true, false).applyTo(txPlace);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).
			grab(true, false).applyTo(txLatitude);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).
			grab(true, false).applyTo(txLongitude);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).
			grab(true, false).applyTo(txZone);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).
			grab(true, false).applyTo(txGreenwich);
	}
	
	/**
	 * Поиск персоны, для которой рассчитывается транзит
	 * @return персона
	 */
	public Event getPerson() {
		return person;
	}

	@Override
	public void reset() {
		dt.setNullText("");
		dt2.setNullText("");
		txPlace.setText(""); //$NON-NLS-1$
		txLatitude.setText(""); //$NON-NLS-1$
		txLongitude.setText(""); //$NON-NLS-1$
		txZone.setText(""); //$NON-NLS-1$
		txGreenwich.setText(""); //$NON-NLS-1$
	}

	public Date getInitialDate() {
		return dt.getSelection();
	}

	public Date getFinalDate() {
		return dt2.getSelection();
	}

	@Override
	public Model createModel() {
		return null;
	}

	@Override
	protected String[] initTableColumns() {
		return null;
	}

	/**
	 * Инициализация представления местности события
	 * @param place местность
	 */
	private void initPlace(Place place) {
		if (null == place) return;
		txPlace.setText(place.getName());
		txLatitude.setText(CalcUtil.formatNumber("###.##", place.getLatitude())); //$NON-NLS-1$
		txLongitude.setText(CalcUtil.formatNumber("###.##", place.getLongitude())); //$NON-NLS-1$
		txGreenwich.setText(CalcUtil.formatNumber("###.##", place.getGreenwich())); //$NON-NLS-1$
		txZone.setText(String.valueOf(place.getGreenwich()));
	}

	/**
	 * Инициализация местностей
	 */
	private void setPlaces() {
	    PlaceProposalProvider proposalProvider = new PlaceProposalProvider();
	    ContentProposalAdapter adapter = new ContentProposalAdapter(
	        txPlace, new TextContentAdapter(),
	        proposalProvider, KeyStroke.getInstance(SWT.CTRL, 32), new char[] {' '});
	    adapter.setPropagateKeys(true);
	    adapter.setProposalAcceptanceStyle(ContentProposalAdapter.PROPOSAL_REPLACE);
	    adapter.addContentProposalListener(new IContentProposalListener() {
			@Override
			public void proposalAccepted(IContentProposal proposal) {
				Place place = (Place)((PlaceContentProposal)proposal).getObject();
				if (place != null) {
					trplace = place;
					initPlace(place);
				}
			}
		});
	}

	@Override
	protected void initControls() {
		setPlaces();
	}

	public Place getPlace() {
		return trplace;
	}

	public double getZone() {
		return Double.parseDouble(txZone.getText());
	}
}
