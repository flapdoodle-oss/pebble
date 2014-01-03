/*******************************************************************************
 * This file is part of Pebble.
 * 
 * Original work Copyright (c) 2009-2013 by the Twig Team
 * Modified work Copyright (c) 2013 by Mitchell Bösecke
 * 
 * For the full copyright and license information, please view the LICENSE
 * file that was distributed with this source code.
 ******************************************************************************/
package com.mitchellbosecke.pebble.node;

import com.mitchellbosecke.pebble.compiler.Compiler;
import com.mitchellbosecke.pebble.node.expression.NodeExpressionArguments;
import com.mitchellbosecke.pebble.node.expression.NodeExpressionDeclaration;

public class NodeMacro extends AbstractNode {

	public static final String MACRO_PREFIX = "macro_";

	private final NodeExpressionArguments args;

	private final String name;

	private final NodeBody body;

	public NodeMacro(int lineNumber, String name, NodeExpressionArguments args, NodeBody body) {
		super(lineNumber);
		this.name = name;
		this.args = args;
		this.body = body;
	}

	@Override
	public void compile(Compiler compiler) {

		/*
		 * Because the macro might exist in a different template we must
		 * manually pass the context and writer as secret arguments.
		 * 
		 * We prefix them with underscores because technically they get put into
		 * the context and are then accessible to the user and we want to avoid
		 * conflicts.
		 * 
		 * TODO: remove the underscore prefix and simultaneously prevent these
		 * two args from being added into context object and are therefore
		 * inaccessible to user.
		 */
		NodeExpressionDeclaration contextDeclaration = new NodeExpressionDeclaration(args.getLineNumber(), "_context");
		NodeExpressionDeclaration writerDeclaration = new NodeExpressionDeclaration(args.getLineNumber(), "_writer");
		args.addArgument(contextDeclaration);
		args.addArgument(writerDeclaration);

		compiler.write(String.format("public void %s%s", MACRO_PREFIX, name)).subcompile(args)
				.raw(" throws com.mitchellbosecke.pebble.error.PebbleException {\n\n").indent();

		/*
		 * Each macro has it's own copy of the main context. We will add the
		 * macro arguments into this new context to give them scope and easy
		 * accessibility.
		 * 
		 * We can't use pushContext() here because the macro might exist in a
		 * completely different template.
		 */
		compiler.write("Context context = new Context(((Context)_context).isStrictVariables());").raw("\n");
		compiler.write("context.setParent((Context)_context);").raw("\n");
		compiler.write("PebbleWrappedWriter writer = (PebbleWrappedWriter)_writer;").raw("\n");

		// put args into scoped context
		for (NodeExpression arg : args.getArgs()) {
			compiler.write("context.put(").string(((NodeExpressionDeclaration) arg).getName()).raw(",")
					.raw(((NodeExpressionDeclaration) arg).getName()).raw(");\n");
		}

		compiler.subcompile(body);

		compiler.raw("\n").outdent().write("}");

	}

}
