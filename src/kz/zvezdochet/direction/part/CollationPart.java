package kz.zvezdochet.direction.part;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import kz.zvezdochet.bean.Event;
import kz.zvezdochet.core.handler.Handler;
import kz.zvezdochet.core.service.DataAccessException;
import kz.zvezdochet.core.ui.comparator.TableSortListenerFactory;
import kz.zvezdochet.core.ui.decoration.InfoDecoration;
import kz.zvezdochet.core.ui.listener.ListSelectionListener;
import kz.zvezdochet.core.ui.util.DialogUtil;
import kz.zvezdochet.core.ui.view.ModelLabelProvider;
import kz.zvezdochet.core.ui.view.ModelPart;
import kz.zvezdochet.core.ui.view.View;
import kz.zvezdochet.core.util.DateUtil;
import kz.zvezdochet.direction.bean.Collation;
import kz.zvezdochet.part.ICalculable;
import kz.zvezdochet.provider.EventProposalProvider;
import kz.zvezdochet.provider.EventProposalProvider.EventContentProposal;

import org.eclipse.jface.bindings.keys.KeyStroke;
import org.eclipse.jface.fieldassist.ContentProposalAdapter;
import org.eclipse.jface.fieldassist.IContentProposal;
import org.eclipse.jface.fieldassist.IContentProposalListener;
import org.eclipse.jface.fieldassist.TextContentAdapter;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;

/**
 * Представление синастрии
 * @author Nataly Didenko
 *
 */
public class CollationPart extends ModelPart implements ICalculable {
	@Inject
	public CollationPart() {}

	private Text txEvent;
	private TableViewer tvParticipants;
	private Table tbParticipants;
	private TableViewer tvMembers;
	private Table tbMembers;

	@PostConstruct @Override
	public View create(Composite parent) {
		// событие
		Label lb = new Label(parent, SWT.NONE);
		lb.setText("Событие");
		txEvent = new Text(parent, SWT.BORDER);
		new InfoDecoration(txEvent, SWT.TOP | SWT.LEFT);
		txEvent.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
		txEvent.setFocus();

		EventProposalProvider proposalProvider = new EventProposalProvider(new Object[] {0});
	    ContentProposalAdapter adapter = new ContentProposalAdapter(
	        txEvent, new TextContentAdapter(),
	        proposalProvider, KeyStroke.getInstance(SWT.CTRL, 32), new char[] {' '});
	    adapter.setPropagateKeys(true);
	    adapter.setProposalAcceptanceStyle(ContentProposalAdapter.PROPOSAL_REPLACE);
	    adapter.addContentProposalListener(new IContentProposalListener() {
			@Override
			public void proposalAccepted(IContentProposal proposal) {
				Event event = (Event)((EventContentProposal)proposal).getObject();
				if (event != null) {
					Collation collation = new Collation();
					collation.setEvent(event);
					setModel(collation, true);
				}
			}
		});

	    // участники
	    Group grParticipants = new Group(parent, SWT.NONE);
		grParticipants.setText("Участники");
		grParticipants.setLayout(new GridLayout());
		grParticipants.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		Text txParticipant = new Text(grParticipants, SWT.BORDER);
		new InfoDecoration(txParticipant, SWT.TOP | SWT.LEFT);
		txParticipant.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

		proposalProvider = new EventProposalProvider(new Object[] {1,2});
	    adapter = new ContentProposalAdapter(
	        txParticipant, new TextContentAdapter(),
	        proposalProvider, KeyStroke.getInstance(SWT.CTRL, 32), new char[] {' '});
	    adapter.setPropagateKeys(true);
	    adapter.setProposalAcceptanceStyle(ContentProposalAdapter.PROPOSAL_REPLACE);
	    adapter.addContentProposalListener(new IContentProposalListener() {
			@Override
			public void proposalAccepted(IContentProposal proposal) {
				Event event = (Event)((EventContentProposal)proposal).getObject();
				if (event != null)
					addParticipant(event);
			}
		});

	    tvParticipants = new TableViewer(grParticipants, SWT.BORDER | SWT.FULL_SELECTION);
		tbParticipants = tvParticipants.getTable();
		tbParticipants.setHeaderVisible(true);
		tbParticipants.setLinesVisible(true);

		String[] columns = new String[] {
			"Имя",
			"Дата" };
		for (String column : columns) {
			TableColumn tableColumn = new TableColumn(tbParticipants, SWT.NONE);
			tableColumn.setText(column);		
			tableColumn.addListener(SWT.Selection, TableSortListenerFactory.getListener(
				TableSortListenerFactory.STRING_COMPARATOR));
		}
		tvParticipants.setContentProvider(new ArrayContentProvider());
		tvParticipants.setLabelProvider(getLabelProvider());

		ListSelectionListener listener = new ListSelectionListener();
		tvParticipants.addSelectionChangedListener(new ISelectionChangedListener() {
			@Override
			public void selectionChanged(SelectionChangedEvent event) {
				if (!event.getSelection().isEmpty()) {
					IStructuredSelection sel = (IStructuredSelection)event.getSelection();
					if (sel.getFirstElement() != null) {
						Event participant = (Event)sel.getFirstElement();
						initMemberTable(participant.getMembers());
					}
				}				
			}
		});
		tvParticipants.addDoubleClickListener(listener);

	    // фигуранты
		Group grMembers = new Group(parent, SWT.NONE);
		grMembers.setText("Фигуранты");
		grMembers.setLayout(new GridLayout());
		grMembers.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		Text txMember = new Text(grMembers, SWT.BORDER);
		new InfoDecoration(txMember, SWT.TOP | SWT.LEFT);
		txMember.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));

		proposalProvider = new EventProposalProvider(new Object[] {1});
	    adapter = new ContentProposalAdapter(
	        txMember, new TextContentAdapter(),
	        proposalProvider, KeyStroke.getInstance(SWT.CTRL, 32), new char[] {' '});
	    adapter.setPropagateKeys(true);
	    adapter.setProposalAcceptanceStyle(ContentProposalAdapter.PROPOSAL_REPLACE);
	    adapter.addContentProposalListener(new IContentProposalListener() {
			@Override
			public void proposalAccepted(IContentProposal proposal) {
				Event event = (Event)((EventContentProposal)proposal).getObject();
				if (event != null)
					addMember(event);
			}
		});

	    tvMembers = new TableViewer(grMembers, SWT.BORDER | SWT.FULL_SELECTION);
		tbMembers = tvMembers.getTable();
		tbMembers.setHeaderVisible(true);
		tbMembers.setLinesVisible(true);

		for (String column : columns) {
			TableColumn tableColumn = new TableColumn(tbMembers, SWT.NONE);
			tableColumn.setText(column);		
			tableColumn.addListener(SWT.Selection, TableSortListenerFactory.getListener(
				TableSortListenerFactory.STRING_COMPARATOR));
		}
		tvMembers.setContentProvider(new ArrayContentProvider());
		tvMembers.setLabelProvider(getLabelProvider());

		listener = new ListSelectionListener();
		tvMembers.addSelectionChangedListener(listener);
		tvMembers.addDoubleClickListener(listener);

		super.create(parent);
		return null;
	}

	/**
	 * Добавление фигуранта события
	 * @param event фигурант
	 */
	protected void addMember(Event event) {
		try {
			IStructuredSelection sel = (IStructuredSelection)tvParticipants.getSelection();
			if (null == sel.getFirstElement()) {
				DialogUtil.alertError("Задайте сообщество, в которое добавляется участник");
				return;
			} else {
				Event participant = (Event)sel.getFirstElement();
				List<Event> members = participant.getMembers();
				if (null == members)
					members = new ArrayList<Event>();
				if (members.contains(event))
					return;
				members.add(event);
				participant.setMembers(members);
				tvMembers.add(event);
				tvMembers.setSelection(new StructuredSelection(event));
			}				
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Добавление участника события
	 * @param event участник
	 */
	protected void addParticipant(Event event) {
		try {
			Collation collation = (Collation)getModel(0, false);
			if (null == collation)
				DialogUtil.alertError("Задайте событие, в которое добавляется участник");
			else {
				if (null == event)
					DialogUtil.alertError("Задайте участника события");
				else {
					List<Event> participants = collation.getParticipants();
					if (null == participants)
						participants = new ArrayList<Event>();
					if (participants.contains(event))
						return;
					participants.add(event);
					collation.setParticipants(participants);
					tvParticipants.add(event);
					tvParticipants.setSelection(new StructuredSelection(event));
				}
			}
		} catch (DataAccessException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public boolean check(int mode) throws Exception {
		if (Handler.MODE_SAVE == mode) {
			if (null == model) {
				DialogUtil.alertError("Выберите событие");
				return false;
			}
		}
		return true;
	}

	private IBaseLabelProvider getLabelProvider() {
		return new ModelLabelProvider() {
			@Override
			public String getColumnText(Object element, int columnIndex) {
				Event event = (Event)element;
				switch (columnIndex) {
					case 0: return event.getName();
					case 1: return DateUtil.formatDateTime(event.getBirth());
				}
				return null;
			}
		};
	}

	@Override
	protected void init(Composite parent) {
		GridLayoutFactory.swtDefaults().applyTo(parent);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(parent);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true, true).applyTo(tbParticipants);
		GridDataFactory.fillDefaults().align(SWT.FILL, SWT.FILL).grab(true, true).applyTo(tbMembers);
	}
	
	@Override
	public void onCalc(Object mode) {
		System.out.println("onCalc" + mode);
		try {
			Event event = (Event)getModel((int)mode, true);
			//TODO расчёт
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void syncView() {
		if (null == model) return;
		Collation collation = (Collation)model;
		if (null == collation.getEvent())
			txEvent.setText("");
		else
			txEvent.setText(collation.getEvent().getName());
		initParticipantTable(collation.getParticipants());
		initMemberTable(null);
	}

	@Override
	public void syncModel(int mode) throws Exception {
		// TODO Auto-generated method stub
		
	}

	/**
	 * Инициализация таблицы участников
	 */
	private void initParticipantTable(List<Event> data) {
		try {
			showBusy(true);
			if (null == data)
				tbParticipants.removeAll();
			else
				tvParticipants.setInput(data);
			for (int i = 0; i < tbParticipants.getColumnCount(); i++)
				tbParticipants.getColumn(i).pack();
		} catch(Exception e) {
			e.printStackTrace();
		} finally {
			showBusy(false);
		}
	}

	/**
	 * Инициализация таблицы фигурантов
	 */
	private void initMemberTable(List<Event> data) {
		try {
			showBusy(true);
			if (null == data)
				tbMembers.removeAll();
			else
				tvMembers.setInput(data);
			for (int i = 0; i < tbMembers.getColumnCount(); i++)
				tbMembers.getColumn(i).pack();
		} catch(Exception e) {
			e.printStackTrace();
		} finally {
			showBusy(false);
		}
	}
}
