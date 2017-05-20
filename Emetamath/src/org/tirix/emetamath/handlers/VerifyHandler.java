package org.tirix.emetamath.handlers;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.expressions.IEvaluationContext;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.tirix.emetamath.nature.MetamathBuilder;
import org.tirix.emetamath.nature.MetamathProjectNature;

import mmj.lang.MessageHandler;

public class VerifyHandler extends MetamathEditorActionHandler {

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		IEvaluationContext context = (IEvaluationContext)event.getApplicationContext();
		MetamathProjectNature nature = getNature(context);
        MessageHandler messageHandler = nature.getMessageHandler();
		Job job = new Job("Verify Metamath Proofs") {
			@Override
			protected IStatus run(IProgressMonitor monitor) {
				MetamathBuilder.doVerifyProof(nature, messageHandler, monitor);
				return Status.OK_STATUS;
			}

		};
		job.schedule();		
		return null; // must return null...
	}
}
